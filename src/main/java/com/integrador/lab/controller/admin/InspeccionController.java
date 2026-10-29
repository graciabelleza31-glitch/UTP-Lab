package com.integrador.lab.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.modelo.Laboratorio;
import com.integrador.lab.modelo.Producto;
import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.modelo.ReservaIntegrante;
import com.integrador.lab.modelo.ReservaItem;
import com.integrador.lab.modelo.Sancion;
import com.integrador.lab.repository.AlumnoRepository;
import com.integrador.lab.repository.LaboratorioRepository;
import com.integrador.lab.repository.ProductoRepository;
import com.integrador.lab.repository.ReservaIntegranteRepository;
import com.integrador.lab.repository.ReservaItemRepository;
import com.integrador.lab.repository.ReservaRepository;
import com.integrador.lab.repository.SancionRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/inspeccion")
public class InspeccionController {

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

    @Autowired
    private LaboratorioRepository laboratorioRepositorio;

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    // 1. VISTA PRINCIPAL: LISTA DE RESERVAS PARA INSPECCIONAR
    @GetMapping
    @Transactional(readOnly = true)
    public String verInspeccion(
            @RequestParam(required = false) String fecha, 
            HttpSession sesion, 
            Model modelo) {

        // 1. Calcular la fecha de consulta
        java.time.LocalDate fechaConsulta = java.time.LocalDate.now();
        if (fecha != null && !fecha.isEmpty()) {
            try {
                fechaConsulta = java.time.LocalDate.parse(fecha);
            } catch (Exception e) {
                fechaConsulta = java.time.LocalDate.now();
            }
        }

        // 2. Traer reservas de ESA fecha (EN_USO, DEVUELTA e INSPECCIONADA)
        List<Reserva> enUso = reservaRepositorio.findByEstado("EN_USO");
        List<Reserva> devueltas = reservaRepositorio.findByEstado("DEVUELTA");
        List<Reserva> inspeccionadas = reservaRepositorio.findByEstado("INSPECCIONADA");
        
        List<Reserva> todasLasPendientes = new ArrayList<>();
        if (enUso != null) todasLasPendientes.addAll(enUso);
        if (devueltas != null) todasLasPendientes.addAll(devueltas);
        if (inspeccionadas != null) todasLasPendientes.addAll(inspeccionadas);

        final java.time.LocalDate fechaFinal = fechaConsulta;

        List<InspeccionCardDto> listaInspeccion = new ArrayList<>();
        for (Reserva r : todasLasPendientes) {
            if (r.getFechaReserva() == null || !r.getFechaReserva().equals(fechaFinal)) {
                continue; 
            }
            listaInspeccion.add(convertirADto(r));
        }

        // 👇 ORDENAR: Primero las que están DEVUELTA (pendientes), luego las INSPECCIONADA (historial)
        listaInspeccion.sort((a, b) -> {
            boolean aEsPendiente = "DEVUELTA".equals(a.getEstado());
            boolean bEsPendiente = "DEVUELTA".equals(b.getEstado());
            if (aEsPendiente && !bEsPendiente) return -1;
            if (!aEsPendiente && bEsPendiente) return 1;
            return 0;
        });


        modelo.addAttribute("inspecciones", listaInspeccion);
        
        
        modelo.addAttribute("fechaConsulta", fechaConsulta.toString());
        modelo.addAttribute("fechaAnterior", fechaConsulta.minusDays(1).toString());
        modelo.addAttribute("fechaSiguiente", fechaConsulta.plusDays(1).toString());

        return "admin/inspeccion";
    }

    // Método auxiliar para convertir Reserva a DTO
    private InspeccionCardDto convertirADto(Reserva r) {
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
                ? r.getLaboratorio().getTipo() : "General";

        String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";

        return new InspeccionCardDto(
                r.getIdReserva(),
                labNombre,
                tipoLab,
                cubiculo,
                (r.getFechaReserva() != null ? r.getFechaReserva().toString() : ""),
                horario,
                r.getEstado(),
                nombresIntegrantes
        );
    }

    // 2. MODAL: DETALLE DE INSPECCIÓN
    @GetMapping("/api/{idReserva}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerDetalleInspeccion(@PathVariable("idReserva") String idReserva) {
        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Reserva r = resOpt.get();
        List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);

        List<ItemInspeccionDto> listaInsumosPrestados = new ArrayList<>();
        List<ItemInspeccionDto> listaEppsSinCosto = new ArrayList<>();

        boolean tieneBata = false;
        boolean tieneGafas = false;

        for (ReservaItem it : items) {
            Producto p = it.getProducto();
            if (p != null) {
                String cat = (p.getCategoria() != null) ? p.getCategoria().trim().toUpperCase() : "";
                BigDecimal reposicion = (p.getCostoReposicion() != null) ? p.getCostoReposicion() : BigDecimal.ZERO;

                if (cat.contains("EPP")) {
                    String nombreMin = p.getNombre().toLowerCase();
                    boolean esDeVenta = (p.getPrecioVenta() != null && p.getPrecioVenta().compareTo(BigDecimal.ZERO) > 0);

                    if (!esDeVenta) {
                        if (nombreMin.contains("bata")) tieneBata = true;
                        if (nombreMin.contains("gafa") || nombreMin.contains("lente")) tieneGafas = true;

                        listaEppsSinCosto.add(new ItemInspeccionDto(
                                p.getIdProducto(),
                                p.getNombre(),
                                reposicion,
                                nombreMin.contains("bata") ? "🥼" : "🥽"
                        ));
                    }
                } else {
                    listaInsumosPrestados.add(new ItemInspeccionDto(
                            p.getIdProducto(),
                            p.getNombre(),
                            reposicion,
                            "📦"
                    ));
                }
            }
        }

        if (!tieneBata) {
            listaEppsSinCosto.add(new ItemInspeccionDto("EPP-OBL-BATA", "Bata de laboratorio", new BigDecimal("50.00"), "🥼"));
        }
        if (!tieneGafas) {
            listaEppsSinCosto.add(new ItemInspeccionDto("EPP-OBL-GAFAS", "Gafas de seguridad", new BigDecimal("25.00"), "🥽"));
        }

        String labNombre = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";
        String horario = (r.getHoraInicio() != null && r.getHoraFin() != null)
                ? r.getHoraInicio() + "–" + r.getHoraFin() : "09:00–11:00";

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("idReserva", r.getIdReserva());
        respuesta.put("laboratorio", labNombre);
        respuesta.put("horario", horario);
        respuesta.put("insumos", listaInsumosPrestados);
        respuesta.put("epps", listaEppsSinCosto);

        return ResponseEntity.ok(respuesta);
    }

    // 3. FINALIZAR INSPECCIÓN
    @PostMapping("/finalizar/{idReserva}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> finalizarInspeccion(
            @PathVariable("idReserva") String idReserva,
            @RequestBody FinalizarInspeccionRequest req) {

        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Reserva no encontrada"));
        }

        Reserva r = resOpt.get();
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
        boolean huboDanios = (req.getDanados() != null && !req.getDanados().isEmpty());

        if (huboDanios) {
            for (ReservaIntegrante ri : integrantes) {
                Alumno alumno = ri.getAlumno();
                if (alumno != null) {
                    alumno.setEstadoCuenta("BLOQUEADO_POR_DEUDA");
                    alumnoRepositorio.save(alumno);

                    for (DanioItemDto d : req.getDanados()) {
                        Producto prodDanado = null;
                        if (d.getIdProducto() != null && !d.getIdProducto().startsWith("EPP-OBL")) {
                            prodDanado = productoRepositorio.findById(d.getIdProducto()).orElse(null);
                        }

                        BigDecimal costoSancion = (d.getCostoReposicion() != null && d.getCostoReposicion().compareTo(BigDecimal.ZERO) > 0)
                                ? d.getCostoReposicion()
                                : (prodDanado != null && prodDanado.getCostoReposicion() != null ? prodDanado.getCostoReposicion() : new BigDecimal("50.00"));

                        String motivo = (d.getMotivo() != null && !d.getMotivo().isBlank())
                                ? d.getMotivo()
                                : "Daño en " + d.getNombre() + " en inspección";

                        int randomTresDigitos = (int) (Math.random() * 900) + 100;
                        String idSancion = "S" + randomTresDigitos;

                        Sancion sancion = new Sancion(
                                idSancion,
                                r,
                                alumno,
                                prodDanado,
                                motivo,
                                costoSancion,
                                "PENDIENTE"
                        );
                        sancion.setFechaSancion(LocalDateTime.now());
                        sancionRepositorio.save(sancion);
                    }
                }
            }
            r.setEstado("CON_INCIDENCIA");
        } else {
            r.setEstado("INSPECCIONADA");
        }

        reservaRepositorio.save(r);

        Laboratorio lab = r.getLaboratorio();
        if (lab != null) {
            laboratorioRepositorio.save(lab);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("huboDanios", huboDanios);
        return ResponseEntity.ok(resp);
    }

    // 4. REGISTRAR SALIDA (CHECK-OUT)
    @PostMapping("/checkout/{idReserva}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> registrarCheckout(@PathVariable("idReserva") String idReserva) {
        Optional<Reserva> resOpt = reservaRepositorio.findById(idReserva);
        if (resOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Reserva no encontrada"));
        }

        Reserva r = resOpt.get();
        if (!"EN_USO".equals(r.getEstado())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "La reserva no está en uso"));
        }

        r.setEstado("DEVUELTA"); 
        reservaRepositorio.save(r);

        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    // DTOs
    public static class InspeccionCardDto {
        private String idReserva;
        private String laboratorio;
        private String tipoLab;
        private String cubiculo;
        private String fecha;
        private String horario;
        private String estado;
        private List<String> integrantes;

        public InspeccionCardDto(String idReserva, String laboratorio, String tipoLab, String cubiculo, String fecha, String horario, String estado, List<String> integrantes) {
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

    public static class ItemInspeccionDto {
        private String id;
        private String nombre;
        private BigDecimal costoReposicion;
        private String emoji;

        public ItemInspeccionDto(String id, String nombre, BigDecimal costoReposicion, String emoji) {
            this.id = id;
            this.nombre = nombre;
            this.costoReposicion = costoReposicion;
            this.emoji = emoji;
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public BigDecimal getCostoReposicion() { return costoReposicion; }
        public String getEmoji() { return emoji; }
    }

    public static class FinalizarInspeccionRequest {
        private List<DanioItemDto> danados;
        public List<DanioItemDto> getDanados() { return danados; }
        public void setDanados(List<DanioItemDto> danados) { this.danados = danados; }
    }

    public static class DanioItemDto {
        private String idProducto;
        private String nombre;
        private BigDecimal costoReposicion;
        private String motivo;

        public String getIdProducto() { return idProducto; }
        public void setIdProducto(String idProducto) { this.idProducto = idProducto; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public BigDecimal getCostoReposicion() { return costoReposicion; }
        public void setCostoReposicion(BigDecimal costoReposicion) { this.costoReposicion = costoReposicion; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }
}