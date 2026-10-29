package com.integrador.lab.controller.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import com.integrador.lab.modelo.Producto;
import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.modelo.ReservaIntegrante;
import com.integrador.lab.modelo.ReservaItem;
import com.integrador.lab.repository.ProductoRepository;
import com.integrador.lab.repository.ReservaIntegranteRepository;
import com.integrador.lab.repository.ReservaItemRepository;
import com.integrador.lab.repository.ReservaRepository;
import com.integrador.lab.repository.SancionRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/checkin")
public class CheckinController {

    @Autowired
    private ReservaRepository reservaRepositorio;

    @Autowired
    private ReservaIntegranteRepository reservaIntegranteRepositorio;

    @Autowired
    private ReservaItemRepository reservaItemRepositorio;

    @Autowired
    private ProductoRepository productoRepositorio;

    @Autowired
    private SancionRepository sancionRepositorio;

    // 1. VISTA PRINCIPAL DE CHECK-IN (CON FILTRO POR FECHA)
    @GetMapping
@Transactional(readOnly = true)
public String verCheckin(
        @RequestParam(required = false) String fecha,
        HttpSession sesion, 
        Model modelo) {
        // 1. Calcular fecha de consulta usando String para evitar conflictos
        String fechaConsultaStr = (fecha != null && !fecha.isEmpty()) ? fecha : java.time.LocalDate.now().toString();
        
                // Declaramos e inicializamos la variable de una vez
        java.time.LocalDate fechaConsulta = java.time.LocalDate.now();
        
        try {
            if (fecha != null && !fecha.isEmpty()) {
                fechaConsulta = java.time.LocalDate.parse(fecha);
            }
        } catch (Exception e) {
            // Si falla el parseo, se queda con el valor de hoy que ya asignamos arriba
            fechaConsulta = java.time.LocalDate.now(); 
        }

    // 2. Traer TODAS las reservas de esa fecha (historial completo)
    List<Reserva> todas = reservaRepositorio.findAll().stream()
            .filter(r -> r.getFechaReserva() != null && r.getFechaReserva().equals(fechaConsulta))
            .filter(r -> !"CANCELADA".equalsIgnoreCase(r.getEstado())) // No mostrar las canceladas
            .sorted((a, b) -> b.getIdReserva().compareTo(a.getIdReserva())) // Ordenar por ID descendente
            .toList();

    // 3. Armar la lista de DTOs
    List<CheckinReservaDto> listaCheckin = new ArrayList<>();

    for (Reserva r : todas) {
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
        List<String> nombresIntegrantes = new ArrayList<>();
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                nombresIntegrantes.add(ri.getAlumno().getNombre() + " " + ri.getAlumno().getApellido());
            }
        }

        String labNombre = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";
        String cubiculo = (r.getLaboratorio() != null && r.getLaboratorio().getUbicacionCubiculo() != null)
                ? r.getLaboratorio().getUbicacionCubiculo() : "S/C";
        String tipoLab = (r.getLaboratorio() != null && r.getLaboratorio().getTipo() != null)
                ? r.getLaboratorio().getTipo() : "Química";

        String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";

        listaCheckin.add(new CheckinReservaDto(
                r.getIdReserva(),
                labNombre,
                tipoLab,
                cubiculo,
                (r.getFechaReserva() != null ? r.getFechaReserva().toString() : ""),
                horario,
                r.getEstado(),
                nombresIntegrantes
        ));
    }

    long incidenciasAbiertas = sancionRepositorio.count();

    modelo.addAttribute("reservas", listaCheckin);
    modelo.addAttribute("totalIncidencias", incidenciasAbiertas > 0 ? incidenciasAbiertas : 1);

    modelo.addAttribute("fechaConsulta", fechaConsulta.toString());
    modelo.addAttribute("fechaAnterior", fechaConsulta.minusDays(1).toString());
    modelo.addAttribute("fechaSiguiente", fechaConsulta.plusDays(1).toString());

    return "admin/checkin";
}

    // 2. ENDPOINT JSON PARA CARGAR EL MODAL CON INTEGRANTES, INSUMOS Y EPPS
    @GetMapping("/api/{idReserva}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerDetalleModal(@PathVariable("idReserva") String idReserva) {
        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Reserva r = resOpt.get();
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
        List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);

        // A. Integrantes
        List<IntegranteDetalleDto> listaEstudiantes = new ArrayList<>();
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                String nom = ri.getAlumno().getNombre();
                String ape = ri.getAlumno().getApellido();
                String iniciales = (nom.length() > 0 ? nom.substring(0, 1) : "")
                        + (ape.length() > 0 ? ape.substring(0, 1) : "");
                listaEstudiantes.add(new IntegranteDetalleDto(
                        ri.getAlumno().getIdEstud(),
                        nom + " " + ape,
                        iniciales.toUpperCase()
                ));
            }
        }

        // B. Insumos y EPPs
        List<ItemDetalleDto> listaInsumos = new ArrayList<>();
        List<ItemDetalleDto> listaEpps = new ArrayList<>();

        boolean tieneBata = false;
        boolean tieneGafas = false;

        for (ReservaItem it : items) {
            Producto p = it.getProducto();
            if (p != null) {
                String cat = (p.getCategoria() != null) ? p.getCategoria().trim().toUpperCase() : "";
                String nom = p.getNombre();
                String unidad = "und";
                if (cat.contains("SOLVENTE") || cat.contains("REACTIVO")) {
                    unidad = "mL";
                } else if (cat.contains("POLVO")) {
                    unidad = "g";
                }

                if (cat.contains("EPP")) {
                    String emoji = "🥽";
                    if (nom.toLowerCase().contains("bata")) {
                        emoji = "🥼";
                        tieneBata = true;
                    }
                    if (nom.toLowerCase().contains("gafa") || nom.toLowerCase().contains("lente")) {
                        tieneGafas = true;
                    }
                    listaEpps.add(new ItemDetalleDto(p.getIdProducto(), nom, unidad, emoji));
                } else {
                    listaInsumos.add(new ItemDetalleDto(p.getIdProducto(), nom, unidad, "📦"));
                }
            }
        }

        // REGLA: Los EPPs sin costo obligatorios (Bata y Gafas) se incluyen para ingresar
        if (!tieneBata) {
            listaEpps.add(new ItemDetalleDto("EPP-OBL-1", "Bata de laboratorio", "und", "🥼"));
        }
        if (!tieneGafas) {
            listaEpps.add(new ItemDetalleDto("EPP-OBL-2", "Gafas de seguridad", "und", "🥽"));
        }

        String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";
        String labNombre = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";

        DetalleModalCheckinDto dto = new DetalleModalCheckinDto(
                r.getIdReserva(),
                labNombre,
                horario,
                listaEstudiantes,
                listaInsumos,
                listaEpps
        );

        return ResponseEntity.ok(dto);
    }

    // 3. CONFIRMAR CHECK-IN VÍA ASÍNCRONA (CAMBIO A "EN_USO")
    @PostMapping("/confirmar/{idReserva}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> confirmarCheckin(@PathVariable("idReserva") String idReserva) {
        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Reserva no encontrada"));
        }

        Reserva r = resOpt.get();
        r.setEstado("EN_USO");
        reservaRepositorio.save(r);

        // Integrantes marcados como asistidos
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
        for (ReservaIntegrante ri : integrantes) {
            ri.setAsistio(true);
            reservaIntegranteRepositorio.save(ri);
        }

        // Ítems marcados como entregados
        List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);
        for (ReservaItem it : items) {
            it.setDevuelto(true);
            reservaItemRepositorio.save(it);
        }

        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    // DTOs Auxiliares
    public static class CheckinReservaDto {

        private String idReserva;
        private String laboratorio;
        private String tipoLab;
        private String cubiculo;
        private String fecha;
        private String horario;
        private String estado;
        private List<String> integrantes;

        public CheckinReservaDto(String idReserva, String laboratorio, String tipoLab, String cubiculo, String fecha, String horario, String estado, List<String> integrantes) {
            this.idReserva = idReserva;
            this.laboratorio = laboratorio;
            this.tipoLab = tipoLab;
            this.cubiculo = cubiculo;
            this.fecha = fecha;
            this.horario = horario;
            this.estado = estado;
            this.integrantes = integrantes;
        }

        public String getIdReserva() { return idReserva; }
        public String getLaboratorio() { return laboratorio; }
        public String getTipoLab() { return tipoLab; }
        public String getCubiculo() { return cubiculo; }
        public String getFecha() { return fecha; }
        public String getHorario() { return horario; }
        public String getEstado() { return estado; }
        public List<String> getIntegrantes() { return integrantes; }
    }

    public static class DetalleModalCheckinDto {

        private String idReserva;
        private String laboratorio;
        private String horario;
        private List<IntegranteDetalleDto> integrantes;
        private List<ItemDetalleDto> insumos;
        private List<ItemDetalleDto> epps;

        public DetalleModalCheckinDto(String idReserva, String laboratorio, String horario, List<IntegranteDetalleDto> integrantes, List<ItemDetalleDto> insumos, List<ItemDetalleDto> epps) {
            this.idReserva = idReserva;
            this.laboratorio = laboratorio;
            this.horario = horario;
            this.integrantes = integrantes;
            this.insumos = insumos;
            this.epps = epps;
        }

        public String getIdReserva() { return idReserva; }
        public String getLaboratorio() { return laboratorio; }
        public String getHorario() { return horario; }
        public List<IntegranteDetalleDto> getIntegrantes() { return integrantes; }
        public List<ItemDetalleDto> getInsumos() { return insumos; }
        public List<ItemDetalleDto> getEpps() { return epps; }
    }

    public static class IntegranteDetalleDto {

        private String codigo;
        private String nombre;
        private String iniciales;

        public IntegranteDetalleDto(String codigo, String nombre, String iniciales) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.iniciales = iniciales;
        }

        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getIniciales() { return iniciales; }
    }

    public static class ItemDetalleDto {

        private String id;
        private String nombre;
        private String unidad;
        private String emoji;

        public ItemDetalleDto(String id, String nombre, String unidad, String emoji) {
            this.id = id;
            this.nombre = nombre;
            this.unidad = unidad;
            this.emoji = emoji;
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public String getUnidad() { return unidad; }
        public String getEmoji() { return emoji; }
    }
}