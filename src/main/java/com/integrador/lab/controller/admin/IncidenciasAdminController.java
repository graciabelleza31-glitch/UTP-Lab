package com.integrador.lab.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.integrador.lab.repository.ReservaRepository;
import com.integrador.lab.repository.SancionRepository;
import com.integrador.lab.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/incidencias")
public class IncidenciasAdminController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SancionRepository sancionRepositorio;

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @Autowired 
    private ReservaRepository reservaRepositorio;

        @GetMapping
    @Transactional(readOnly = true)
    public String verIncidencias(
            @RequestParam(required = false) String fecha,
            HttpSession sesion, 
            Model modelo) {

        // 1. Calcular fecha de consulta SIN usar variables de tipo LocalDate
        String fechaConsultaStr = fecha;
        if (fechaConsultaStr == null || fechaConsultaStr.isEmpty()) {
            fechaConsultaStr = java.time.LocalDate.now().toString();
        }

        // Validar que la fecha sea parseable
        java.time.LocalDate fechaConsulta = java.time.LocalDate.now();
        try {
            fechaConsulta = java.time.LocalDate.parse(fechaConsultaStr);
        } catch (Exception e) {
            fechaConsulta = java.time.LocalDate.now();
            fechaConsultaStr = fechaConsulta.toString();
        }

        // 2. Traer todas las sanciones y filtrar por la fecha
        List<Sancion> todas = sancionRepositorio.findAll();
        
        // Usamos la variable fechaConsulta ya parseada
        final java.time.LocalDate fechaFinal = fechaConsulta;
        
        List<Sancion> sancionesDelDia = todas.stream()
                .filter(s -> s.getFechaSancion() != null && s.getFechaSancion().toLocalDate().equals(fechaFinal))
                .collect(Collectors.toList());

        long cantAbiertas = 0;
        long cantNotificadas = 0;
        long cantResueltas = 0;
        BigDecimal totalMontoPendiente = BigDecimal.ZERO;

        // 3. Agrupar por reserva
        Map<String, List<Sancion>> sancionesPorReserva = sancionesDelDia.stream()
                .collect(Collectors.groupingBy(s -> s.getReserva() != null ? s.getReserva().getIdReserva() : "SIN_RESERVA"));

        List<IncidenciaCardDto> listaIncidencias = new ArrayList<>();

        for (Map.Entry<String, List<Sancion>> entry : sancionesPorReserva.entrySet()) {
            List<Sancion> sancionesDelGrupo = entry.getValue();
            Sancion primera = sancionesDelGrupo.get(0);

            boolean todasResueltas = sancionesDelGrupo.stream()
                    .allMatch(s -> "RESUELTA".equalsIgnoreCase(s.getEstado()) || 
                                   "RESUELTO".equalsIgnoreCase(s.getEstado()) || 
                                   "PAGADO".equalsIgnoreCase(s.getEstado()));

            BigDecimal montoGrupo = sancionesDelGrupo.stream()
                    .filter(s -> !"RESUELTA".equalsIgnoreCase(s.getEstado()) && !"PAGADO".equalsIgnoreCase(s.getEstado()))
                    .map(Sancion::getMontoDeuda)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (!todasResueltas) {
                totalMontoPendiente = totalMontoPendiente.add(montoGrupo);
            }

            List<String> nombresAlumnos = sancionesDelGrupo.stream()
                    .filter(s -> s.getAlumno() != null)
                    .map(s -> s.getAlumno().getNombre() + " " + s.getAlumno().getApellido())
                    .distinct()
                    .collect(Collectors.toList());
            
            List<String> correos = sancionesDelGrupo.stream()
                    .filter(s -> s.getAlumno() != null)
                    .map(s -> s.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe")
                    .distinct()
                    .collect(Collectors.toList());

            boolean isNotif = sancionesDelGrupo.stream()
                    .anyMatch(s -> "NOTIFICADA".equalsIgnoreCase(s.getEstado()) || 
                                   "NOTIFICADO".equalsIgnoreCase(s.getEstado()));

            if (todasResueltas) {
                cantResueltas += sancionesDelGrupo.size();
            } else if (isNotif) {
                cantNotificadas += sancionesDelGrupo.size();
            } else {
                cantAbiertas += sancionesDelGrupo.size();
            }

            String labNombre = (primera.getReserva() != null && primera.getReserva().getLaboratorio() != null) 
                    ? primera.getReserva().getLaboratorio().getNombre() : "Lab. General";
            
            String itemNombre = (primera.getProducto() != null) ? primera.getProducto().getNombre() : "Material / Equipo";
            String motivo = (primera.getMotivo() != null && !primera.getMotivo().isBlank()) ? primera.getMotivo() : "Daño durante práctica";
            
            // Usamos nombre completo para evitar conflictos
            String strFechaNotif = primera.getFechaSancion() != null ? primera.getFechaSancion().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";

            listaIncidencias.add(new IncidenciaCardDto(
                    entry.getKey(),
                    itemNombre,
                    motivo,
                    labNombre,
                    String.join(", ", nombresAlumnos),
                    String.join(", ", correos),
                    "S/ " + montoGrupo.setScale(2).toString(),
                    todasResueltas ? "RESUELTA" : (isNotif ? "NOTIFICADA" : "PENDIENTE"),
                    isNotif,
                    strFechaNotif,
                    todasResueltas
            ));
        }

        // 4. Pasar datos al modelo
        modelo.addAttribute("incidencias", listaIncidencias);
        modelo.addAttribute("cantAbiertas", cantAbiertas);
        modelo.addAttribute("cantNotificadas", cantNotificadas);
        modelo.addAttribute("cantResueltas", cantResueltas);
        modelo.addAttribute("totalPendiente", "S/ " + totalMontoPendiente.setScale(2).toString());
        modelo.addAttribute("totalIncidencias", cantAbiertas + cantNotificadas);

        // Usamos el String para el modelo
        modelo.addAttribute("fechaConsulta", fechaConsultaStr);
        modelo.addAttribute("fechaAnterior", fechaConsulta.minusDays(1).toString());
        modelo.addAttribute("fechaSiguiente", fechaConsulta.plusDays(1).toString());

        return "admin/incidencias";
    }
    // ACCIÓN 1: NOTIFICAR A TODO EL GRUPO
    @PostMapping("/notificar/{idReserva}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> notificarGrupo(@PathVariable("idReserva") String idReserva) {
        List<Sancion> sanciones = sancionRepositorio.findAll().stream()
                .filter(s -> s.getReserva() != null && s.getReserva().getIdReserva().equals(idReserva))
                .collect(Collectors.toList());

        for (Sancion s : sanciones) {
            if (!"RESUELTA".equalsIgnoreCase(s.getEstado()) && !"NOTIFICADA".equalsIgnoreCase(s.getEstado())) {
                s.setEstado("NOTIFICADA");
                sancionRepositorio.save(s);

                Alumno al = s.getAlumno();
                if (al != null) {
                    al.setEstadoCuenta("BLOQUEADO_POR_DEUDA");
                    alumnoRepositorio.save(al);

                    try {
                        String correoDestino = al.getIdEstud().toLowerCase() + "@utp.edu.pe";
                        String nombreCompleto = al.getNombre() + " " + al.getApellido();
                        emailService.enviarCorreoIncidencia(
                            correoDestino, 
                            nombreCompleto, 
                            s.getMotivo(), 
                            s.getMontoDeuda().toString(), 
                            idReserva
                        );
                    } catch (Exception e) {
                        System.out.println("❌ Error enviando correo: " + e.getMessage());
                    }
                }
            }
        }
        
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return ResponseEntity.ok(Collections.singletonMap("fechaNotificado", fechaActual));
    }

    // ACCIÓN 2: RESOLVER TODO EL GRUPO
    @PostMapping("/resolver/{idReserva}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> resolverGrupo(@PathVariable("idReserva") String idReserva) {
        List<Sancion> sanciones = sancionRepositorio.findAll().stream()
                .filter(s -> s.getReserva() != null && s.getReserva().getIdReserva().equals(idReserva))
                .collect(Collectors.toList());

        for (Sancion s : sanciones) {
            s.setEstado("RESUELTA");
            sancionRepositorio.save(s);

            Alumno al = s.getAlumno();
            if (al != null) {
                long pendientes = sancionRepositorio.countByAlumno_IdEstudAndEstadoNotIn(
                    al.getIdEstud(), 
                    List.of("RESUELTA")
                );
                if (pendientes == 0) {
                    al.setEstadoCuenta("HABILITADO");
                    alumnoRepositorio.save(al);
                }
            }
        }
        reservaRepositorio.findById(idReserva).ifPresent(reserva -> {
            reserva.setEstado("INSPECCIONADA"); 
            reservaRepositorio.save(reserva);
        });
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    // DTO actualizado con el campo 'resuelta'
    public static class IncidenciaCardDto {
        private String idSancion;
        private String item;
        private String motivo;
        private String laboratorio;
        private String estudiante;
        private String correo;
        private String costo;
        private String estado;
        private boolean notificada;
        private String fechaNotificado;
        private boolean resuelta; // 👈 NUEVO CAMPO

        public IncidenciaCardDto(String idSancion, String item, String motivo, String laboratorio, String estudiante, String correo, String costo, String estado, boolean notificada, String fechaNotificado, boolean resuelta) {
            this.idSancion = idSancion;
            this.item = item;
            this.motivo = motivo;
            this.laboratorio = laboratorio;
            this.estudiante = estudiante;
            this.correo = correo;
            this.costo = costo;
            this.estado = estado;
            this.notificada = notificada;
            this.fechaNotificado = fechaNotificado;
            this.resuelta = resuelta;
        }

        public String getIdSancion() { return idSancion; }
        public String getItem() { return item; }
        public String getMotivo() { return motivo; }
        public String getLaboratorio() { return laboratorio; }
        public String getEstudiante() { return estudiante; }
        public String getCorreo() { return correo; }
        public String getCosto() { return costo; }
        public String getEstado() { return estado; }
        public boolean isNotificada() { return notificada; }
        public String getFechaNotificado() { return fechaNotificado; }
        public boolean isResuelta() { return resuelta; }
    }
}