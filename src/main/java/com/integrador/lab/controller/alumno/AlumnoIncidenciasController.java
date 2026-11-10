package com.integrador.lab.controller.alumno;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.modelo.Sancion;
import com.integrador.lab.repository.AlumnoRepository;
import com.integrador.lab.repository.SancionRepository;
import com.integrador.lab.service.PagoSimulacionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/alumno/incidencias")
public class AlumnoIncidenciasController {

    @Autowired
    private SancionRepository sancionRepositorio;

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @GetMapping
public String verMisIncidencias(
        @RequestParam(required = false) String mes,
        HttpSession session, 
        Model model) {
    
    Alumno alumnoLogueado = (Alumno) session.getAttribute("usuarioLogueado");
    if (alumnoLogueado == null) {
        return "redirect:/login";
    }

    // 1. Calcular la fecha del mes seleccionado
    java.time.LocalDate fechaConsulta;
    try {
        fechaConsulta = (mes != null && !mes.isEmpty()) 
                ? java.time.LocalDate.parse(mes + "-01") 
                : java.time.LocalDate.now();
    } catch (Exception e) {
        fechaConsulta = java.time.LocalDate.now();
    }

    java.time.LocalDate hoy = java.time.LocalDate.now();
    java.time.LocalDate mesAnterior = fechaConsulta.minusMonths(1);
    java.time.LocalDate mesSiguiente = fechaConsulta.plusMonths(1);
    
    java.util.Locale espLocale = new java.util.Locale("es", "PE");
    String nombreMes = fechaConsulta.getMonth().getDisplayName(java.time.format.TextStyle.FULL, espLocale);
    nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    String mesFormateado = nombreMes + " " + fechaConsulta.getYear();
    
    String mesInput = String.format("%04d-%02d", fechaConsulta.getYear(), fechaConsulta.getMonthValue());

    List<SancionAlumnoDto> listaIncidencias = new ArrayList<>();
    int pendientesCount = 0;
    boolean estaBloqueado = false;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    List<Sancion> misSanciones = sancionRepositorio.findByAlumno_IdEstud(alumnoLogueado.getIdEstud());

    for (Sancion s : misSanciones) {
        String est = (s.getEstado() != null) ? s.getEstado().toUpperCase().trim() : "PENDIENTE";
        boolean resuelta = "RESUELTA".equals(est) || "PAGADO".equals(est) || "PAGADA".equals(est);

        // 👇 YA NO HACEMOS CONTINUE AQUÍ. Las resueltas también se muestran.
        
        if (!resuelta) {
            pendientesCount++;
        }

        // ⭐ FILTRAR POR MES
        if (s.getReserva() != null && s.getReserva().getFechaReserva() != null) {
            int mesSancion = s.getReserva().getFechaReserva().getMonthValue();
            int anioSancion = s.getReserva().getFechaReserva().getYear();
            
            if (mesSancion != fechaConsulta.getMonthValue() || anioSancion != fechaConsulta.getYear()) {
                continue; // Si no es del mes, lo saltamos
            }
        }

        String item = (s.getProducto() != null) ? s.getProducto().getNombre() : "Material / Equipo";
        String motivo = (s.getMotivo() != null) ? s.getMotivo() : "Daño reportado";
        String lab = (s.getReserva() != null && s.getReserva().getLaboratorio() != null) 
                     ? s.getReserva().getLaboratorio().getNombre() : "Lab. Química";
        String idReserva = (s.getReserva() != null) ? s.getReserva().getIdReserva() : "R-001";
        String fechaReserva = (s.getFechaSancion() != null) ? s.getFechaSancion().format(dtf) : LocalDateTime.now().format(dtf);
        BigDecimal monto = (s.getMontoDeuda() != null) ? s.getMontoDeuda() : BigDecimal.ZERO;
        boolean notificada = "NOTIFICADA".equalsIgnoreCase(est);

        listaIncidencias.add(new SancionAlumnoDto(
                s.getIdSancion(),
                item,
                motivo,
                lab,
                idReserva,
                fechaReserva,
                "S/ " + monto.setScale(2).toString(),
                resuelta ? "Pagada" : (notificada ? "Notificada" : "Pendiente"),
                resuelta, // 👈 Le pasamos si está resuelta
                (s.getFechaSancion() != null) ? s.getFechaSancion().toString() : "—"
        ));
    }

    estaBloqueado = (pendientesCount > 0) || "BLOQUEADO_POR_DEUDA".equalsIgnoreCase(alumnoLogueado.getEstadoCuenta());

    model.addAttribute("alumno", alumnoLogueado);
    model.addAttribute("incidencias", listaIncidencias);
    model.addAttribute("pendientesCount", pendientesCount);
    model.addAttribute("estaBloqueado", estaBloqueado);

    // Datos del selector de mes
    model.addAttribute("mesFormateado", mesFormateado);
    model.addAttribute("mesInput", mesInput);
    model.addAttribute("mesAnterior", String.format("%04d-%02d", mesAnterior.getYear(), mesAnterior.getMonthValue()));
    model.addAttribute("mesSiguiente", String.format("%04d-%02d", mesSiguiente.getYear(), mesSiguiente.getMonthValue()));
    model.addAttribute("esMesActual", fechaConsulta.getMonthValue() == hoy.getMonthValue() && fechaConsulta.getYear() == hoy.getYear());

    return "alumno/incidencias";
}

    @Autowired
    private PagoSimulacionService pagoSimulacionService;


    @Autowired
    private com.integrador.lab.repository.ReservaIntegranteRepository reservaIntegranteRepositorio;

    // =========================================================================
    // ACCIÓN: CONFIRMAR PAGO DE LA SANCIÓN POR PARTE DEL ALUMNO
    // =========================================================================
    @PostMapping("/pagar/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> registrarPagoAlumno(
            @PathVariable("id") String id,
            @RequestParam(value = "metodoPago", defaultValue = "YAPE") String metodoPago,
            @RequestParam(value = "numeroTarjeta", required = false) String numeroTarjeta,
            @RequestParam(value = "titularTarjeta", required = false) String titularTarjeta,
            @RequestParam(value = "fechaExpTarjeta", required = false) String fechaExpTarjeta,
            @RequestParam(value = "cvvTarjeta", required = false) String cvvTarjeta,
            @RequestParam(value = "numeroYape", required = false) String numeroYape,
            @RequestParam(value = "codigoYape", required = false) String codigoYape,
            @RequestParam(value = "numeroPlin", required = false) String numeroPlin,
            @RequestParam(value = "codigoPlin", required = false) String codigoPlin,
            HttpSession session) {

        Optional<Sancion> opt = sancionRepositorio.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sanción no encontrada"));
        }

        Sancion s = opt.get();

        // ⭐ NUEVA FIRMA con Yape y Plin
        PagoSimulacionService.ResultadoPago resultado =
                pagoSimulacionService.procesarPago(
                        metodoPago,
                        numeroTarjeta, titularTarjeta, fechaExpTarjeta, cvvTarjeta,
                        numeroYape, codigoYape,
                        numeroPlin, codigoPlin,
                        s.getMontoDeuda());

        if (!resultado.isExitoso()) {
            return ResponseEntity.badRequest().body(Map.of("error", resultado.getMensaje()));
        }

        // Si el pago es exitoso:
        com.integrador.lab.modelo.Reserva reservaDeLaSancion = s.getReserva();

        if (reservaDeLaSancion != null) {
            String idReserva = reservaDeLaSancion.getIdReserva();

            List<Sancion> sancionesDeLaReserva = sancionRepositorio.findAll().stream()
                    .filter(x -> x.getReserva() != null && x.getReserva().getIdReserva().equals(idReserva))
                    .toList();

            for (Sancion x : sancionesDeLaReserva) {
                x.setEstado("PAGADA");
                sancionRepositorio.save(x);
            }

            // Desbloquear a TODOS los integrantes de la reserva si no tienen otras deudas
            List<com.integrador.lab.modelo.ReservaIntegrante> integrantes =
                    reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);

            for (com.integrador.lab.modelo.ReservaIntegrante ri : integrantes) {
                Alumno al = ri.getAlumno();
                if (al == null) continue;

                long pendientes = sancionRepositorio.countByAlumno_IdEstudAndEstadoNotIn(
                        al.getIdEstud(), List.of("RESUELTA", "PAGADA"));

                if (pendientes == 0) {
                    al.setEstadoCuenta("HABILITADO");
                    alumnoRepositorio.save(al);
                }
            }
        } else {
            s.setEstado("PAGADA");
            sancionRepositorio.save(s);

            Alumno al = s.getAlumno();
            if (al != null) {
                long deudasPendientes = sancionRepositorio.countByAlumno_IdEstudAndEstadoNotIn(
                        al.getIdEstud(), List.of("RESUELTA", "PAGADA"));
                if (deudasPendientes == 0) {
                    al.setEstadoCuenta("HABILITADO");
                    alumnoRepositorio.save(al);
                }
            }
        }

        // Refrescar la sesión del alumno logueado
        Alumno alumnoSesion = (Alumno) session.getAttribute("usuarioLogueado");
        if (alumnoSesion != null) {
            Alumno alumnoFresco = alumnoRepositorio.findById(alumnoSesion.getIdEstud()).orElse(null);
            if (alumnoFresco != null) {
                session.setAttribute("usuarioLogueado", alumnoFresco);
            }
        }

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "transaccion", resultado.getTransaccion() != null ? resultado.getTransaccion() : "TXN-000",
            "mensaje", resultado.getMensaje()
        ));
    }

        public static class SancionAlumnoDto {
        private String idSancion;
        private String item;
        private String motivo;
        private String laboratorio;
        private String reserva;
        private String fecha;
        private String costo;
        private String estado;
        private boolean resuelto;
        private String fechaNotificado;

        public SancionAlumnoDto(String idSancion, String item, String motivo, String laboratorio, String reserva, String fecha, String costo, String estado, boolean resuelto, String fechaNotificado) {
            this.idSancion = idSancion;
            this.item = item;
            this.motivo = motivo;
            this.laboratorio = laboratorio;
            this.reserva = reserva;
            this.fecha = fecha;
            this.costo = costo;
            this.estado = estado;
            this.resuelto = resuelto;
            this.fechaNotificado = fechaNotificado;
        }

        public String getIdSancion() { return idSancion; }
        public String getItem() { return item; }
        public String getMotivo() { return motivo; }
        public String getLaboratorio() { return laboratorio; }
        public String getReserva() { return reserva; }
        public String getFecha() { return fecha; }
        public String getCosto() { return costo; }
        public String getEstado() { return estado; }
        public boolean isResuelto() { return resuelto; }
        public String getFechaNotificado() { return fechaNotificado; }
    }
}
