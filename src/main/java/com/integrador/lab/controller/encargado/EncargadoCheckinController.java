package com.integrador.lab.controller.encargado;

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

import com.integrador.lab.modelo.Encargado;
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
@RequestMapping("/encargado/checkin")
public class EncargadoCheckinController {

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

       // 1. VISTA PRINCIPAL
    @GetMapping
    @Transactional(readOnly = true)
    public String verCheckin(
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String buscar,
            HttpSession sesion,
            Model modelo) {

        if (sesion.getAttribute("usuarioLogueado") == null) return "redirect:/login";

        java.time.LocalDate fechaConsulta = java.time.LocalDate.now();
        try {
            if (fecha != null && !fecha.isEmpty()) {
                fechaConsulta = java.time.LocalDate.parse(fecha);
            }
        } catch (Exception e) {
            fechaConsulta = java.time.LocalDate.now();
        }

        final java.time.LocalDate fechaFinal = fechaConsulta;
        final String textoBusqueda = (buscar != null) ? buscar.toLowerCase().trim() : "";

        List<Reserva> todas = reservaRepositorio.findAll().stream()
                // 👇 SI HAY BÚSQUEDA, IGNORAMOS EL FILTRO DE FECHA 👇
                .filter(r -> {
                    if (!textoBusqueda.isEmpty()) return true; 
                    return r.getFechaReserva() != null && r.getFechaReserva().equals(fechaFinal);
                })
                .filter(r -> !"CANCELADA".equalsIgnoreCase(r.getEstado()))
                .sorted((a, b) -> b.getIdReserva().compareTo(a.getIdReserva()))
                .toList();

        List<CheckinReservaDto> listaCheckin = new ArrayList<>();
        for (Reserva r : todas) {
            List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(r.getIdReserva());
            List<String> nombresIntegrantes = new ArrayList<>();
            List<String> codigosIntegrantes = new ArrayList<>();
            for (ReservaIntegrante ri : integrantes) {
                if (ri.getAlumno() != null) {
                    nombresIntegrantes.add(ri.getAlumno().getNombre() + " " + ri.getAlumno().getApellido());
                    // Extraemos el código del alumno
                    if (ri.getAlumno().getIdEstud() != null) {
                        codigosIntegrantes.add(ri.getAlumno().getIdEstud().toLowerCase());
                    }
                }
            }

            String labNombre = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";
            String cubiculo = (r.getLaboratorio() != null && r.getLaboratorio().getUbicacionCubiculo() != null)
                    ? r.getLaboratorio().getUbicacionCubiculo() : "S/C";
            String tipoLab = (r.getLaboratorio() != null && r.getLaboratorio().getTipo() != null)
                    ? r.getLaboratorio().getTipo() : "Química";

            String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                    ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";

                        // 👇 FILTRO EN MEMORIA POR SI EL USUARIO BUSCA UN NOMBRE, LAB, CÓDIGO DE RESERVA O CÓDIGO DE ALUMNO 👇
            if (!textoBusqueda.isEmpty()) {
                boolean coincide = r.getIdReserva().toLowerCase().contains(textoBusqueda) ||
                                   labNombre.toLowerCase().contains(textoBusqueda) ||
                                   nombresIntegrantes.stream().anyMatch(n -> n.toLowerCase().contains(textoBusqueda)) ||
                                   codigosIntegrantes.stream().anyMatch(c -> c.contains(textoBusqueda)); // 👈 NUEVA CONDICIÓN
                if (!coincide) continue; 
            }

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

        modelo.addAttribute("reservas", listaCheckin);
        modelo.addAttribute("fechaConsulta", fechaConsulta.toString());
        modelo.addAttribute("fechaAnterior", fechaConsulta.minusDays(1).toString());
        modelo.addAttribute("fechaSiguiente", fechaConsulta.plusDays(1).toString());
        modelo.addAttribute("buscar", buscar); // 👈 Enviamos el texto al HTML para que no se borre del input

        return "encargado/checkin";
    }

    // 2. ENDPOINT AJAX: DETALLE DE RESERVA
    @GetMapping("/api/{idReserva}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerDetalleModal(@PathVariable("idReserva") String idReserva) {
        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) return ResponseEntity.notFound().build();

        Reserva r = resOpt.get();
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
        List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);

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
                if (cat.contains("SOLVENTE") || cat.contains("REACTIVO")) unidad = "mL";
                else if (cat.contains("POLVO")) unidad = "g";

                if (cat.contains("EPP")) {
                    String emoji = "🥽";
                    if (nom.toLowerCase().contains("bata")) { emoji = "🥼"; tieneBata = true; }
                    if (nom.toLowerCase().contains("gafa") || nom.toLowerCase().contains("lente")) tieneGafas = true;
                    listaEpps.add(new ItemDetalleDto(p.getIdProducto(), nom, unidad, emoji));
                } else {
                    listaInsumos.add(new ItemDetalleDto(p.getIdProducto(), nom, unidad, "📦"));
                }
            }
        }

        if (!tieneBata) listaEpps.add(new ItemDetalleDto("EPP-OBL-1", "Bata de laboratorio", "und", "🥼"));
        if (!tieneGafas) listaEpps.add(new ItemDetalleDto("EPP-OBL-2", "Gafas de seguridad", "und", "🥽"));

        String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";
        String labNombre = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";

        DetalleModalCheckinDto dto = new DetalleModalCheckinDto(
                r.getIdReserva(), labNombre, horario,
                listaEstudiantes, listaInsumos, listaEpps
        );

        return ResponseEntity.ok(dto);
    }

    // 3. CONFIRMAR CHECK-IN
    @PostMapping("/confirmar/{idReserva}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> confirmarCheckin(@PathVariable("idReserva") String idReserva, HttpSession sesion) {
        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Reserva no encontrada"));
        }

        Reserva r = resOpt.get();
        r.setEstado("EN_USO");
        Object userSession = sesion.getAttribute("usuarioLogueado");
        System.out.println("DEBUG: El objeto en sesión es de tipo: " + (userSession != null ? userSession.getClass().getName() : "NULL"));
    if (userSession instanceof Encargado) {
        r.setEncargadoCheckin((Encargado) userSession);
    }
    r.setFechaHoraCheckin(java.time.LocalDateTime.now());
        reservaRepositorio.save(r);

        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
        for (ReservaIntegrante ri : integrantes) {
            ri.setAsistio(true);
            reservaIntegranteRepositorio.save(ri);
        }

        List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);
        for (ReservaItem it : items) {
            it.setDevuelto(true);
            reservaItemRepositorio.save(it);
        }

        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    // ============ DTOs ============
    public static class CheckinReservaDto {
        private String idReserva, laboratorio, tipoLab, cubiculo, fecha, horario, estado;
        private List<String> integrantes;
        public CheckinReservaDto(String idReserva, String laboratorio, String tipoLab, String cubiculo,
                                 String fecha, String horario, String estado, List<String> integrantes) {
            this.idReserva = idReserva; this.laboratorio = laboratorio; this.tipoLab = tipoLab;
            this.cubiculo = cubiculo; this.fecha = fecha; this.horario = horario;
            this.estado = estado; this.integrantes = integrantes;
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
        private String idReserva, laboratorio, horario;
        private List<IntegranteDetalleDto> integrantes;
        private List<ItemDetalleDto> insumos, epps;
        public DetalleModalCheckinDto(String idReserva, String laboratorio, String horario,
                                      List<IntegranteDetalleDto> integrantes,
                                      List<ItemDetalleDto> insumos, List<ItemDetalleDto> epps) {
            this.idReserva = idReserva; this.laboratorio = laboratorio; this.horario = horario;
            this.integrantes = integrantes; this.insumos = insumos; this.epps = epps;
        }
        public String getIdReserva() { return idReserva; }
        public String getLaboratorio() { return laboratorio; }
        public String getHorario() { return horario; }
        public List<IntegranteDetalleDto> getIntegrantes() { return integrantes; }
        public List<ItemDetalleDto> getInsumos() { return insumos; }
        public List<ItemDetalleDto> getEpps() { return epps; }
    }

    public static class IntegranteDetalleDto {
        private String codigo, nombre, iniciales;
        public IntegranteDetalleDto(String codigo, String nombre, String iniciales) {
            this.codigo = codigo; this.nombre = nombre; this.iniciales = iniciales;
        }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getIniciales() { return iniciales; }
    }

    public static class ItemDetalleDto {
        private String id, nombre, unidad, emoji;
        public ItemDetalleDto(String id, String nombre, String unidad, String emoji) {
            this.id = id; this.nombre = nombre; this.unidad = unidad; this.emoji = emoji;
        }
        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public String getUnidad() { return unidad; }
        public String getEmoji() { return emoji; }
    }
}