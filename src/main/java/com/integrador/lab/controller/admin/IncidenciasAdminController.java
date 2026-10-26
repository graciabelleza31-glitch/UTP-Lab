package com.integrador.lab.controller.admin;

import java.math.BigDecimal;
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
    public String verIncidencias(HttpSession sesion, Model modelo) {
        List<Sancion> todas = sancionRepositorio.findAll();
        
        long cantAbiertas = 0;
        long cantNotificadas = 0;
        long cantResueltas = 0;
        BigDecimal totalMontoPendiente = BigDecimal.ZERO;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 1. Agrupamos TODAS las sanciones por ID de Reserva
        Map<String, List<Sancion>> sancionesPorReserva = todas.stream()
                .collect(Collectors.groupingBy(s -> s.getReserva() != null ? s.getReserva().getIdReserva() : "SIN_RESERVA"));

        List<IncidenciaCardDto> listaIncidencias = new ArrayList<>();

        for (Map.Entry<String, List<Sancion>> entry : sancionesPorReserva.entrySet()) {
            List<Sancion> sancionesDelGrupo = entry.getValue();
            Sancion primera = sancionesDelGrupo.get(0);

            // Verificamos si el grupo entero ya está resuelto
            boolean todasResueltas = sancionesDelGrupo.stream()
                    .allMatch(s -> "RESUELTA".equalsIgnoreCase(s.getEstado()) || "RESUELTO".equalsIgnoreCase(s.getEstado()) || "PAGADO".equalsIgnoreCase(s.getEstado()));

            if (todasResueltas) {
                cantResueltas += sancionesDelGrupo.size();
                continue; // No se muestran en la lista activa
            }

            // Sumamos el monto total de las sanciones no resueltas del grupo
            BigDecimal montoGrupo = sancionesDelGrupo.stream()
                    .filter(s -> !"RESUELTA".equalsIgnoreCase(s.getEstado()) && !"PAGADO".equalsIgnoreCase(s.getEstado()))
                    .map(Sancion::getMontoDeuda)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalMontoPendiente = totalMontoPendiente.add(montoGrupo);

            // Juntamos los nombres de los alumnos afectados y sus correos
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

            String alumnosStr = String.join(", ", nombresAlumnos);
            String correosStr = String.join(", ", correos);

            // Verificamos si el grupo ya fue notificado (si al menos una está NOTIFICADA)
            boolean isNotif = sancionesDelGrupo.stream()
                    .anyMatch(s -> "NOTIFICADA".equalsIgnoreCase(s.getEstado()) || "NOTIFICADO".equalsIgnoreCase(s.getEstado()));

            if (isNotif) {
                cantNotificadas += sancionesDelGrupo.size();
            } else {
                cantAbiertas += sancionesDelGrupo.size();
            }

            String labNombre = (primera.getReserva() != null && primera.getReserva().getLaboratorio() != null) 
                    ? primera.getReserva().getLaboratorio().getNombre() : "Lab. General";
            
            String itemNombre = (primera.getProducto() != null) ? primera.getProducto().getNombre() : "Material / Equipo";
            String motivo = (primera.getMotivo() != null && !primera.getMotivo().isBlank()) ? primera.getMotivo() : "Daño durante práctica";
            String strFechaNotif = LocalDateTime.now().minusDays(1).format(dtf);

            listaIncidencias.add(new IncidenciaCardDto(
                    entry.getKey(), // Usamos el ID de la reserva como identificador de la tarjeta
                    itemNombre,
                    motivo,
                    labNombre,
                    alumnosStr,
                    correosStr,
                    "S/ " + montoGrupo.setScale(2).toString(),
                    isNotif ? "Notificada" : "Pendiente",
                    isNotif,
                    strFechaNotif
            ));
        }

        modelo.addAttribute("incidencias", listaIncidencias);
        modelo.addAttribute("cantAbiertas", cantAbiertas);
        modelo.addAttribute("cantNotificadas", cantNotificadas);
        modelo.addAttribute("cantResueltas", cantResueltas);
        modelo.addAttribute("totalPendiente", "S/ " + totalMontoPendiente.setScale(2).toString());
        modelo.addAttribute("totalIncidencias", cantAbiertas + cantNotificadas);

        return "admin/incidencias";
    }

       // ACCIÓN 1: NOTIFICAR A TODO EL GRUPO (PASA A ESTADO 'NOTIFICADA' Y ENVÍA CORREO)
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

                    // 📧 ENVÍO DE CORREO AL ESTUDIANTE CON BREVO
                    try {
                        String correoDestino = al.getIdEstud().toLowerCase() + "@utp.edu.pe";
                        String nombreCompleto = al.getNombre() + " " + al.getApellido();
                        
                        // Llamamos al nuevo método de tu EmailService
                        emailService.enviarCorreoIncidencia(
                            correoDestino, 
                            nombreCompleto, 
                            s.getMotivo(), 
                            s.getMontoDeuda().toString(), 
                            idReserva
                        );
                        
                    } catch (Exception e) {
                        System.out.println("❌ Error enviando correo a " + al.getIdEstud() + ": " + e.getMessage());
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

    public static class IncidenciaCardDto {
        private String idSancion; // En este caso guardará el ID de la Reserva
        private String item;
        private String motivo;
        private String laboratorio;
        private String estudiante;
        private String correo;
        private String costo;
        private String estado;
        private boolean notificada;
        private String fechaNotificado;

        public IncidenciaCardDto(String idSancion, String item, String motivo, String laboratorio, String estudiante, String correo, String costo, String estado, boolean notificada, String fechaNotificado) {
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
    }
}