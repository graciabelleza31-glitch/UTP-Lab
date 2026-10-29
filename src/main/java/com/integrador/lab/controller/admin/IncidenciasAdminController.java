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

        // 1. Calcular la fecha de consulta (si es null, usaremos "hoy" para el selector visual)
        java.time.LocalDate fechaConsulta = java.time.LocalDate.now();
        if (fecha != null && !fecha.isEmpty()) {
            try {
                fechaConsulta = java.time.LocalDate.parse(fecha);
            } catch (Exception e) {
                fechaConsulta = java.time.LocalDate.now();
            }
        }
        final java.time.LocalDate fechaFinal = fechaConsulta;

        // 2. Traer TODAS las sanciones de la BD
        List<Sancion> todas = sancionRepositorio.findAll();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // =====================================================================
        // SECCIÓN 1: TODAS LAS ACTIVAS (Sin importar la fecha)
        // =====================================================================
        List<Sancion> activas = todas.stream()
                .filter(s -> !"RESUELTA".equalsIgnoreCase(s.getEstado()) && !"PAGADA".equalsIgnoreCase(s.getEstado()))
                .collect(Collectors.toList());

        Map<String, List<Sancion>> activasPorReserva = activas.stream()
                .collect(Collectors.groupingBy(s -> s.getReserva() != null ? s.getReserva().getIdReserva() : "SIN_RESERVA"));

        List<IncidenciaCardDto> listaActivas = new ArrayList<>();
        long cantAbiertas = 0;
        long cantNotificadas = 0;

        for (Map.Entry<String, List<Sancion>> entry : activasPorReserva.entrySet()) {
            List<Sancion> sancionesDelGrupo = entry.getValue();
            Sancion primera = sancionesDelGrupo.get(0);

            // Contadores
            boolean isNotif = sancionesDelGrupo.stream()
                    .anyMatch(s -> "NOTIFICADA".equalsIgnoreCase(s.getEstado()) || "NOTIFICADO".equalsIgnoreCase(s.getEstado()));

            if (isNotif) {
                cantNotificadas += sancionesDelGrupo.size();
            } else {
                cantAbiertas += sancionesDelGrupo.size();
            }

            // Datos
            BigDecimal montoGrupo = sancionesDelGrupo.stream()
                    .map(Sancion::getMontoDeuda)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            List<String> nombresAlumnos = sancionesDelGrupo.stream()
                    .filter(s -> s.getAlumno() != null)
                    .map(s -> s.getAlumno().getNombre() + " " + s.getAlumno().getApellido())
                    .distinct().collect(Collectors.toList());
            
            List<String> correos = sancionesDelGrupo.stream()
                    .filter(s -> s.getAlumno() != null)
                    .map(s -> s.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe")
                    .distinct().collect(Collectors.toList());

            String labNombre = (primera.getReserva() != null && primera.getReserva().getLaboratorio() != null) 
                    ? primera.getReserva().getLaboratorio().getNombre() : "Lab. General";
            String itemNombre = (primera.getProducto() != null) ? primera.getProducto().getNombre() : "Material / Equipo";
            String motivo = (primera.getMotivo() != null && !primera.getMotivo().isBlank()) ? primera.getMotivo() : "Daño durante práctica";
            String strFechaNotif = primera.getFechaSancion() != null ? primera.getFechaSancion().format(dtf) : "";

            listaActivas.add(new IncidenciaCardDto(
                    entry.getKey(), itemNombre, motivo, labNombre,
                    String.join(", ", nombresAlumnos), String.join(", ", correos),
                    "S/ " + montoGrupo.setScale(2).toString(),
                    isNotif ? "NOTIFICADA" : "PENDIENTE",
                    isNotif, strFechaNotif, false
            ));
        }

        // =====================================================================
        // SECCIÓN 2: HISTORIAL (Solo RESUELTAS, filtradas por fecha)
        // =====================================================================
        List<Sancion> resueltas = todas.stream()
                .filter(s -> "RESUELTA".equalsIgnoreCase(s.getEstado()) || "PAGADA".equalsIgnoreCase(s.getEstado()))
                .filter(s -> s.getFechaSancion() != null && s.getFechaSancion().toLocalDate().equals(fechaFinal))
                .collect(Collectors.toList());

        Map<String, List<Sancion>> resueltasPorReserva = resueltas.stream()
                .collect(Collectors.groupingBy(s -> s.getReserva() != null ? s.getReserva().getIdReserva() : "SIN_RESERVA"));

        List<IncidenciaCardDto> listaHistorial = new ArrayList<>();
        long cantResueltas = 0;

        for (Map.Entry<String, List<Sancion>> entry : resueltasPorReserva.entrySet()) {
            List<Sancion> sancionesDelGrupo = entry.getValue();
            Sancion primera = sancionesDelGrupo.get(0);
            cantResueltas += sancionesDelGrupo.size();

            BigDecimal montoGrupo = sancionesDelGrupo.stream()
                    .map(Sancion::getMontoDeuda)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            List<String> nombresAlumnos = sancionesDelGrupo.stream()
                    .filter(s -> s.getAlumno() != null)
                    .map(s -> s.getAlumno().getNombre() + " " + s.getAlumno().getApellido())
                    .distinct().collect(Collectors.toList());
            
            List<String> correos = sancionesDelGrupo.stream()
                    .filter(s -> s.getAlumno() != null)
                    .map(s -> s.getAlumno().getIdEstud().toLowerCase() + "@utp.edu.pe")
                    .distinct().collect(Collectors.toList());

            String labNombre = (primera.getReserva() != null && primera.getReserva().getLaboratorio() != null) 
                    ? primera.getReserva().getLaboratorio().getNombre() : "Lab. General";
            String itemNombre = (primera.getProducto() != null) ? primera.getProducto().getNombre() : "Material / Equipo";
            String motivo = (primera.getMotivo() != null && !primera.getMotivo().isBlank()) ? primera.getMotivo() : "Daño durante práctica";
            String strFechaNotif = primera.getFechaSancion() != null ? primera.getFechaSancion().format(dtf) : "";

            listaHistorial.add(new IncidenciaCardDto(
                    entry.getKey(), itemNombre, motivo, labNombre,
                    String.join(", ", nombresAlumnos), String.join(", ", correos),
                    "S/ " + montoGrupo.setScale(2).toString(),
                    "RESUELTA",
                    true, strFechaNotif, true
            ));
        }

        // 3. Pasar datos al modelo
        modelo.addAttribute("incidenciasActivas", listaActivas);
        modelo.addAttribute("incidenciasHistorial", listaHistorial);
        
        modelo.addAttribute("cantAbiertas", cantAbiertas);
        modelo.addAttribute("cantNotificadas", cantNotificadas);
        modelo.addAttribute("cantResueltas", cantResueltas);
        
        // Total global para la campanita (todas las activas)
        long totalGlobalActivas = activas.size();
        modelo.addAttribute("totalIncidencias", totalGlobalActivas);

        modelo.addAttribute("fechaConsulta", fechaConsulta.toString());
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
        private boolean resuelta;

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