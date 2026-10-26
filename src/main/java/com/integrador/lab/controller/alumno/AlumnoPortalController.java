package com.integrador.lab.controller.alumno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.integrador.lab.repository.ReservaItemRepository;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.integrador.lab.controller.alumno.AlumnoPortalController.HistorialReservaDto;
import com.integrador.lab.controller.alumno.AlumnoPortalController.ReservaActivaDto;
import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.modelo.Boleta;
import com.integrador.lab.modelo.Laboratorio;
import com.integrador.lab.modelo.Producto;
import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.modelo.ReservaIntegrante;
import com.integrador.lab.modelo.ReservaItem;
import com.integrador.lab.modelo.Sancion;
import com.integrador.lab.repository.AlumnoRepository;
import com.integrador.lab.repository.BoletaRepository;
import com.integrador.lab.repository.LaboratorioRepository;
import com.integrador.lab.repository.ProductoRepository;
import com.integrador.lab.repository.ReservaIntegranteRepository;
import com.integrador.lab.repository.ReservaItemRepository;
import com.integrador.lab.repository.ReservaRepository;
import com.integrador.lab.repository.SancionRepository;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/alumno")
public class AlumnoPortalController {

    @Autowired
    private LaboratorioRepository laboratorioRepositorio;

    @Autowired
    private ReservaRepository reservaRepositorio;

    @Autowired
    private ReservaIntegranteRepository reservaIntegranteRepositorio;

    @Autowired
    private ReservaItemRepository reservaItemRepositorio;

    @Autowired
    private ProductoRepository productoRepositorio;

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @Autowired
    private BoletaRepository boletaRepositorio;

    @Autowired
    private SancionRepository sancionRepositorio;


    // =========================================================================
    // 1. PANTALLA PRINCIPAL: LABORATORIOS Y GESTIÓN DE RESERVAS
    // =========================================================================
        @GetMapping("/laboratorios")
    @Transactional(readOnly = true)
    public String verLaboratorios(HttpSession sesion, Model modelo) {
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Alumno alumno = (Alumno) usuario;

        // 1. Consultar sanciones pendientes
        List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
        long incidenciasPendientes = sancionesPendientes.size();

        String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
        boolean estaBloqueado = incidenciasPendientes > 0
                || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta);

        // 2. Obtener la carrera y validar si permite laboratorios
        String idCarrera = (alumno.getCarrera() != null) ? alumno.getCarrera().getIdCarrera() : null;
        
        List<Laboratorio> labs = new ArrayList<>();
        boolean puedeReservarPorCarrera = false;

        if (idCarrera != null) {
            // Usamos la consulta que ya tienes en tu repositorio
            labs = laboratorioRepositorio.findLaboratoriosPermitidosPorCarrera(idCarrera);
            // Si la lista trae al menos un laboratorio, su carrera sí tiene permiso
            puedeReservarPorCarrera = !labs.isEmpty();
        }

        // 3. Separar productos entre Insumos y EPPs
        List<Producto> todosLosProductos = productoRepositorio.findAll();
        List<Producto> insumos = new ArrayList<>();
        List<Producto> epps = new ArrayList<>();

        for (Producto p : todosLosProductos) {
            if (p.getCategoria() != null && "EPP".equalsIgnoreCase(p.getCategoria().trim())) {
                epps.add(p);
            } else {
                insumos.add(p);
            }
        }

        // 4. Contar reservas del día
        List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
        LocalDate hoy = LocalDate.now();
        List<String> idsReservasHoy = new ArrayList<>();

        for (ReservaIntegrante ri : participaciones) {
            Reserva r = ri.getReserva();
            if (r == null || r.getFechaReserva() == null || !hoy.equals(r.getFechaReserva())) continue;

            String estado = r.getEstado() == null ? "" : r.getEstado().trim().toUpperCase();
            if (estado.equals("CANCELADA") || estado.equals("CANCELADO")) continue;

            if (!idsReservasHoy.contains(r.getIdReserva())) {
                idsReservasHoy.add(r.getIdReserva());
            }
        }

        int reservasHoy = idsReservasHoy.size();

        modelo.addAttribute("reservasHoy", reservasHoy);
        modelo.addAttribute("reservasRestantes", Math.max(0, 2 - reservasHoy));
        modelo.addAttribute("limiteDiarioAlcanzado", reservasHoy >= 2);

        // 5. Listar mis reservas activas
        List<ReservaActivaDto> misReservas = new ArrayList<>();
        for (ReservaIntegrante ri : participaciones) {
            Reserva r = ri.getReserva();
            if (r != null && r.getFechaReserva() != null && !r.getFechaReserva().isBefore(LocalDate.now())) {
                String estado = r.getEstado() != null ? r.getEstado().toUpperCase() : "";
                if ("CONFIRMADA".equals(estado) || "EN_USO".equals(estado)) {
                    misReservas.add(new ReservaActivaDto(
                            r.getIdReserva(),
                            r.getLaboratorio() != null ? r.getLaboratorio().getNombre() : "Laboratorio",
                            r.getLaboratorio() != null ? r.getLaboratorio().getUbicacionCubiculo() : "S/C",
                            r.getFechaReserva().toString(),
                            r.getHoraInicio() + "–" + r.getHoraFin(),
                            r.getEstado()
                    ));
                }
            }
        }

        // 6. Enviar todo a la vista
        modelo.addAttribute("alumno", alumno);
        modelo.addAttribute("estaBloqueado", estaBloqueado);
        modelo.addAttribute("numIncidencias", incidenciasPendientes);
        modelo.addAttribute("laboratorios", labs);
        modelo.addAttribute("puedeReservarPorCarrera", puedeReservarPorCarrera); 
        modelo.addAttribute("insumos", insumos);
        modelo.addAttribute("epps", epps);
        modelo.addAttribute("misReservas", misReservas);

        return "alumno-laboratorios";
    }

    // =========================================================================
    // 2. VISTA INDEPENDIENTE: CATÁLOGO DE INSUMOS
    // =========================================================================
    @GetMapping("/catalogos")
    @Transactional(readOnly = true)
    public String verCatalogo(HttpSession sesion, Model modelo) {
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Alumno alumno = null;
        if (usuario instanceof Alumno) {
            alumno = (Alumno) usuario;
        } else {
            return "redirect:/login";
        }

        // Consultar incidencias para la barra lateral y campana
        List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
        long incidenciasPendientes = sancionesPendientes.size();

        String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
        boolean estaBloqueado = incidenciasPendientes > 0
                || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta)
                || estadoCuenta.contains("BLOQUEADO_POR_DEUDA");

        // Obtener todos los insumos de la base de datos
        List<Producto> todosLosProductos = productoRepositorio.findAll();

        // Extraer dinámicamente las categorías únicas existentes en la BD
        List<String> categoriasBD = todosLosProductos.stream()
                .map(Producto::getCategoria)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();

        modelo.addAttribute("alumno", alumno);
        modelo.addAttribute("estaBloqueado", estaBloqueado);
        modelo.addAttribute("numIncidencias", incidenciasPendientes);
        modelo.addAttribute("productos", todosLosProductos);
        modelo.addAttribute("categorias", categoriasBD);
        modelo.addAttribute("totalItems", todosLosProductos.size());

        return "alumno-catalogos";
    }

    // =========================================================================
    // 3. PROCESAR RESERVA, INTEGRANTES, ÍTEMS, STOCK Y BOLETA
    // =========================================================================
    @PostMapping("/reservas/confirmar-pago")
    @Transactional
    public String procesarPagoYReserva(
            @RequestParam("idLab") String idLab,
            @RequestParam("fecha") String fechaStr,
            @RequestParam("horaInicio") String horaInicioStr,
            @RequestParam("horaFin") String horaFinStr,
            @RequestParam("integrantesCodigos") List<String> codigosIntegrantes,
            @RequestParam(value = "insumosIds", required = false) List<String> insumosIds,
            @RequestParam(value = "eppsIds", required = false) List<String> eppsIds,
            @RequestParam("montoTotal") BigDecimal montoTotal,
            @RequestParam(value = "metodoPago", defaultValue = "GRATUITO") String metodoPago,
            HttpSession sesion, RedirectAttributes redirectAttributes) {

        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        Alumno alumnoLogueado = (Alumno) usuario;

        // Blindaje: Validar que la carrera del alumno permita reservar
        String idCarreraAlumno = (alumnoLogueado.getCarrera() != null) ? alumnoLogueado.getCarrera().getIdCarrera() : null;
        if (idCarreraAlumno == null) return "redirect:/alumno/laboratorios";
        
        List<Laboratorio> labsPermitidos = laboratorioRepositorio.findLaboratoriosPermitidosPorCarrera(idCarreraAlumno);
        boolean labPermitido = labsPermitidos.stream().anyMatch(l -> l.getIdLab().equals(idLab));
        if (!labPermitido) {
            redirectAttributes.addFlashAttribute("errorReserva", "Tu carrera no tiene acceso a este laboratorio.");
            return "redirect:/alumno/laboratorios";
        }

        // Doble validación de seguridad contra cuentas bloqueadas
        boolean tieneSancion = sancionRepositorio.existsByAlumno_IdEstudAndEstado(alumnoLogueado.getIdEstud(), "PENDIENTE");
        String estadoCuentaAlumno = (alumnoLogueado.getEstadoCuenta() != null) ? alumnoLogueado.getEstadoCuenta().trim().toUpperCase() : "";
        boolean cuentaBloqueada = "BLOQUEADO_POR_DEUDA".equals(estadoCuentaAlumno) || estadoCuentaAlumno.contains("BLOQUEADO");

        if (tieneSancion || cuentaBloqueada) {
            return "redirect:/alumno/laboratorios";
        }

        Optional<Laboratorio> labOpt = laboratorioRepositorio.findById(idLab);
        if (labOpt.isEmpty()) {
            return "redirect:/alumno/laboratorios";
        }
        Laboratorio lab = labOpt.get();

        LocalDate fecha;
LocalTime horaInicio;
LocalTime horaFin;

try {
    fecha = LocalDate.parse(fechaStr);
    horaInicio = LocalTime.parse(horaInicioStr);
    horaFin = LocalTime.parse(horaFinStr);
} catch (DateTimeParseException e) {
    return "redirect:/alumno/laboratorios";
}

// Validar que la fecha no sea anterior al día actual.
if (fecha.isBefore(LocalDate.now())) {
    return "redirect:/alumno/laboratorios";
}

// La hora de fin debe ser posterior a la hora de inicio.
if (!horaFin.isAfter(horaInicio)) {
    return "redirect:/alumno/laboratorios";
}

        // =====================================================
        // 1. LÍMITE DE DOS RESERVAS DIARIAS POR ALUMNO Y SUS INTEGRANTES
        // =====================================================

        // A. Validar al DUEÑO de la reserva (Alumno logueado)
        List<ReservaIntegrante> participacionesAlumno = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumnoLogueado.getIdEstud());
        List<String> idsReservasDelDia = new ArrayList<>();

        for (ReservaIntegrante participacion : participacionesAlumno) {
            Reserva reservaExistente = participacion.getReserva();
            if (reservaExistente == null || reservaExistente.getFechaReserva() == null || !fecha.equals(reservaExistente.getFechaReserva())) continue;
            
            String estado = reservaExistente.getEstado() == null ? "" : reservaExistente.getEstado().trim().toUpperCase();
            if (estado.equals("CANCELADA") || estado.equals("CANCELADO")) continue;

            if (!idsReservasDelDia.contains(reservaExistente.getIdReserva())) {
                idsReservasDelDia.add(reservaExistente.getIdReserva());
            }
        }

        if (idsReservasDelDia.size() >= 2) {
            redirectAttributes.addFlashAttribute("errorReserva", "Ya alcanzaste el límite de dos reservas para este día. Las reservas canceladas no cuentan.");
            return "redirect:/alumno/laboratorios";
        }

        // B. ⭐ NUEVO: Validar a CADA INTEGRANTE de la reserva
        if (codigosIntegrantes != null) {
            for (String codIntegrante : codigosIntegrantes) {
                // Si el integrante es el mismo dueño, lo saltamos (ya lo validamos arriba)
                if (codIntegrante.trim().equalsIgnoreCase(alumnoLogueado.getIdEstud().trim())) {
                    continue;
                }

                // Buscamos las participaciones del integrante
                List<ReservaIntegrante> participacionesIntegrante = reservaIntegranteRepositorio.findByAlumno_IdEstud(codIntegrante.trim());
                List<String> idsReservasIntegrante = new ArrayList<>();

                for (ReservaIntegrante part : participacionesIntegrante) {
                    Reserva r = part.getReserva();
                    if (r == null || r.getFechaReserva() == null || !fecha.equals(r.getFechaReserva())) continue;

                    String est = r.getEstado() == null ? "" : r.getEstado().trim().toUpperCase();
                    if (est.equals("CANCELADA") || est.equals("CANCELADO")) continue;

                    if (!idsReservasIntegrante.contains(r.getIdReserva())) {
                        idsReservasIntegrante.add(r.getIdReserva());
                    }
                }

                // Si el integrante ya tiene 2 reservas ese día, bloqueamos la reserva completa
                if (idsReservasIntegrante.size() >= 2) {
                    redirectAttributes.addFlashAttribute("errorReserva", 
                        "No se puede completar la reserva. El alumno con código " + codIntegrante + " ya alcanzó su límite de 2 reservas para este día.");
                    return "redirect:/alumno/laboratorios";
                }
            }
        }

// =====================================================
// 2. EVITAR SUPERPOSICIONES EN EL MISMO LABORATORIO
// =====================================================

List<Reserva> reservasDelLaboratorio =
        reservaRepositorio.findByLaboratorio_IdLabAndFechaReserva(
                idLab, fecha
        );

for (Reserva reservaExistente : reservasDelLaboratorio) {
    String estado = reservaExistente.getEstado() == null
            ? ""
            : reservaExistente.getEstado().trim().toUpperCase();

    // Solo bloquean las reservas activas.
    boolean reservaActiva =
            estado.equals("CONFIRMADA")
            || estado.equals("EN_USO");

    if (!reservaActiva) {
        continue;
    }

    LocalTime inicioExistente = reservaExistente.getHoraInicio();
    LocalTime finExistente = reservaExistente.getHoraFin();

    if (inicioExistente == null || finExistente == null) {
        continue;
    }

    // Hay cruce si la nueva reserva empieza antes de que termine
    // la existente y termina después de que esta empieza.
    boolean haySuperposicion =
            horaInicio.isBefore(finExistente)
            && horaFin.isAfter(inicioExistente);

    if (haySuperposicion) {
    redirectAttributes.addFlashAttribute(
        "errorReserva",
        "¡Cruce de horario! El laboratorio ya tiene una reserva activa durante el horario seleccionado. Elige otra hora."
    );
    return "redirect:/alumno/laboratorios";
}
}

        // A. Generación secuencial de 6 caracteres: RES001, RES002, ..., RES006
        String idReserva = "RES001";
        Optional<String> ultimoIdOpt = reservaRepositorio.findUltimoIdReserva();

        if (ultimoIdOpt.isPresent()) {
            try {
                String ultimoId = ultimoIdOpt.get().trim();
                int num = Integer.parseInt(ultimoId.substring(3));
                idReserva = String.format("RES%03d", num + 1);
            } catch (Exception e) {
                long count = reservaRepositorio.count() + 1;
                idReserva = String.format("RES%03d", count);
            }
        }

        Reserva nuevaReserva = new Reserva(idReserva, lab, fecha, horaInicio, horaFin, "CONFIRMADA");
        reservaRepositorio.save(nuevaReserva);

        // B. Guardar Responsable (Alumno logueado)
        ReservaIntegrante responsable = new ReservaIntegrante(nuevaReserva, alumnoLogueado, false);
        reservaIntegranteRepositorio.save(responsable);

        // C. Guardar Integrantes adicionales
        if (codigosIntegrantes != null) {
            for (String cod : codigosIntegrantes) {
                if (!cod.trim().equalsIgnoreCase(alumnoLogueado.getIdEstud().trim())) {
                    alumnoRepositorio.findById(cod.trim()).ifPresent(al -> {
                        reservaIntegranteRepositorio.save(new ReservaIntegrante(nuevaReserva, al, false));
                    });
                }
            }
        }

        // D. Guardar Insumos y EPPs + Descontar Stock Físico
        List<String> productosElegidos = new ArrayList<>();
        if (insumosIds != null) {
            productosElegidos.addAll(insumosIds);
        }
        if (eppsIds != null) {
            productosElegidos.addAll(eppsIds);
        }

        for (String idProd : productosElegidos) {
            productoRepositorio.findById(idProd).ifPresent(prod -> {
                ReservaItem item = new ReservaItem(nuevaReserva, prod, 1, false);
                reservaItemRepositorio.save(item);

                // Descuento en la tabla producto
                if (prod.getStockActual() != null && prod.getStockActual() > 0) {
                    prod.setStockActual(prod.getStockActual() - 1);
                    productoRepositorio.save(prod);
                }
            });
        }

        // E. Generación correlativa de Boleta de 6 caracteres: BOL001, BOL002...
        long totalBoletas = boletaRepositorio.count() + 1;
        String idBoleta = String.format("BOL%03d", totalBoletas);

        Boleta boleta = new Boleta(
                idBoleta,
                alumnoLogueado,
                null,
                nuevaReserva,
                LocalDateTime.now(),
                montoTotal,
                "PAGO_" + metodoPago.toUpperCase()
        );
        boletaRepositorio.save(boleta);

        return "redirect:/alumno/laboratorios";
    }

    // =========================================================================
    // 4. VISTA INDEPENDIENTE: PROTOCOLOS DE SEGURIDAD
    // =========================================================================
    @GetMapping("/protocolos")
    @Transactional(readOnly = true)
    public String verProtocolos(HttpSession sesion, Model modelo) {
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        Alumno alumno = null;
        if (usuario instanceof Alumno) {
            alumno = (Alumno) usuario;
        } else {
            return "redirect:/login";
        }

        // Validación de sanciones y deudas para mostrar la alerta amarilla inferior
        List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
        long incidenciasPendientes = sancionesPendientes.size();

        String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
        boolean estaBloqueado = incidenciasPendientes > 0
                || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta)
                || estadoCuenta.contains("BLOQUEADO_POR_DEUDA");

        modelo.addAttribute("alumno", alumno);
        modelo.addAttribute("estaBloqueado", estaBloqueado);
        modelo.addAttribute("numIncidencias", incidenciasPendientes);

        return "alumno-protocolos";
    }

    // =========================================================================
// 5. VISTA: MI CUENTA (DASHBOARD DEL ESTUDIANTE)
// =========================================================================
@GetMapping("/cuenta")
@Transactional(readOnly = true)
public String verCuenta(
        @RequestParam(required = false) String mes,
        HttpSession sesion, 
        Model modelo) {
    
    Object usuario = sesion.getAttribute("usuarioLogueado");
    if (usuario == null) return "redirect:/login";

    Alumno alumno = null;
    if (usuario instanceof Alumno) {
        alumno = (Alumno) usuario;
    } else {
        return "redirect:/login";
    }

    // ============ FECHA DE CONSULTA (mes seleccionado) ============
    LocalDate fechaConsulta;
    try {
        fechaConsulta = (mes != null && !mes.isEmpty()) 
                ? LocalDate.parse(mes + "-01") 
                : LocalDate.now();
    } catch (Exception e) {
        fechaConsulta = LocalDate.now();
    }
    
    LocalDate hoy = LocalDate.now();
    LocalDate mesAnterior = fechaConsulta.minusMonths(1);
    LocalDate mesSiguiente = fechaConsulta.plusMonths(1);
    
    java.util.Locale espLocale = new java.util.Locale("es", "PE");
    String nombreMes = fechaConsulta.getMonth().getDisplayName(java.time.format.TextStyle.FULL, espLocale);
    nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    String mesFormateado = nombreMes + " " + fechaConsulta.getYear();
    
    // Formato para el input type="month" (YYYY-MM)
    String mesInput = String.format("%04d-%02d", fechaConsulta.getYear(), fechaConsulta.getMonthValue());

    // ============ SANCIONES ============
    List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
    long incidenciasPendientes = sancionesPendientes.size();

    String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
    boolean estaBloqueado = incidenciasPendientes > 0
            || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta)
            || estadoCuenta.contains("BLOQUEADO_POR_DEUDA");

    // ============ HISTORIAL Y ESTADÍSTICAS ============
    List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
    List<HistorialReservaDto> historial = new ArrayList<>();
    
    int reservasDelMes = 0;
    int horasTotales = 0;
    int materialesTotales = 0;

    for (ReservaIntegrante ri : participaciones) {
        Reserva r = ri.getReserva();
        if (r == null || r.getFechaReserva() == null) continue;

        // Formatear fecha: "18 Ago 2026 · 09:00–11:00"
        String dia = String.valueOf(r.getFechaReserva().getDayOfMonth());
        String mesAbrev = r.getFechaReserva().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, espLocale);
        mesAbrev = mesAbrev.substring(0, 1).toUpperCase() + mesAbrev.substring(1).replace(".", "");
        String fechaFormato = dia + " " + mesAbrev + " " + r.getFechaReserva().getYear();
        
        String horario = "";
        if (r.getHoraInicio() != null && r.getHoraFin() != null) {
            horario = r.getHoraInicio() + "–" + r.getHoraFin();
        }

        // Filtrar por mes seleccionado
        boolean esDelMes = r.getFechaReserva().getMonth() == fechaConsulta.getMonth() 
                        && r.getFechaReserva().getYear() == fechaConsulta.getYear();

        // Contar solo las del mes seleccionado
        if (esDelMes) {
            reservasDelMes++;
            if (r.getHoraInicio() != null && r.getHoraFin() != null) {
                long horas = java.time.Duration.between(r.getHoraInicio(), r.getHoraFin()).toHours();
                horasTotales += (int) horas;
            }
            List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(r.getIdReserva());
            materialesTotales += items.size();
        }

        // Solo agregar al historial si es del mes seleccionado
        if (esDelMes) {
            String labNombre = (r.getLaboratorio() != null) ? r.getLaboratorio().getNombre() : "Laboratorio";
            historial.add(new HistorialReservaDto(
                    r.getIdReserva(),
                    labNombre,
                    fechaFormato,
                    horario,
                    r.getEstado()
            ));
        }
    }

    // Ordenar historial descendente por fecha
    historial.sort((a, b) -> b.getIdReserva().compareTo(a.getIdReserva()));

    modelo.addAttribute("alumno", alumno);
    modelo.addAttribute("estaBloqueado", estaBloqueado);
    modelo.addAttribute("numIncidencias", incidenciasPendientes);
    modelo.addAttribute("reservasDelMes", reservasDelMes);
    modelo.addAttribute("horasTotales", horasTotales);
    modelo.addAttribute("materialesTotales", materialesTotales);
    modelo.addAttribute("historial", historial);
    
    // Datos de fecha
    modelo.addAttribute("mesFormateado", mesFormateado);
    modelo.addAttribute("mesInput", mesInput);
    modelo.addAttribute("mesAnterior", String.format("%04d-%02d", mesAnterior.getYear(), mesAnterior.getMonthValue()));
    modelo.addAttribute("mesSiguiente", String.format("%04d-%02d", mesSiguiente.getYear(), mesSiguiente.getMonthValue()));
    modelo.addAttribute("esMesActual", fechaConsulta.getMonth() == hoy.getMonth() && fechaConsulta.getYear() == hoy.getYear());

    // Traer TODAS las incidencias del alumno (no solo las pendientes)
    List<Sancion> todasLasIncidencias = sancionRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
    
    // Ordenarlas de la más reciente a la más antigua (opcional pero recomendado)
    todasLasIncidencias.sort((a, b) -> b.getIdSancion().compareTo(a.getIdSancion()));

    modelo.addAttribute("historialIncidencias", todasLasIncidencias);
    return "alumno-cuenta";
}

@GetMapping("/api/recibo/{idReserva}")
@org.springframework.web.bind.annotation.ResponseBody
@Transactional(readOnly = true)
public java.util.Map<String, Object> obtenerRecibo(@PathVariable("idReserva") String idReserva) {
    java.util.Map<String, Object> resp = new java.util.HashMap<>();
    
    Optional<Reserva> reservaOpt = reservaRepositorio.findById(idReserva);
    if (reservaOpt.isEmpty()) {
        resp.put("encontrado", false);
        return resp;
    }
    
    Reserva r = reservaOpt.get();
    
    // Datos básicos
    resp.put("encontrado", true);
    resp.put("idReserva", r.getIdReserva());
    resp.put("laboratorio", r.getLaboratorio() != null ? r.getLaboratorio().getNombre() : "Laboratorio");
    resp.put("cubiculo", r.getLaboratorio() != null ? r.getLaboratorio().getUbicacionCubiculo() : "S/C");
    resp.put("fecha", r.getFechaReserva() != null ? r.getFechaReserva().toString() : "");
    resp.put("horaInicio", r.getHoraInicio() != null ? r.getHoraInicio().toString() : "");
    resp.put("horaFin", r.getHoraFin() != null ? r.getHoraFin().toString() : "");
    resp.put("estado", r.getEstado());
    
    // Integrantes
    List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
    List<java.util.Map<String, String>> listaIntegrantes = new ArrayList<>();
    for (ReservaIntegrante ri : integrantes) {
        if (ri.getAlumno() != null) {
            java.util.Map<String, String> m = new java.util.HashMap<>();
            m.put("nombre", ri.getAlumno().getNombre() + " " + ri.getAlumno().getApellido());
            m.put("codigo", ri.getAlumno().getIdEstud());
            listaIntegrantes.add(m);
        }
    }
    resp.put("integrantes", listaIntegrantes);
    
    // Insumos y EPPs
    List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);
    List<java.util.Map<String, String>> listaItems = new ArrayList<>();
    for (ReservaItem item : items) {
        if (item.getProducto() != null) {
            java.util.Map<String, String> m = new java.util.HashMap<>();
            m.put("nombre", item.getProducto().getNombre());
            m.put("categoria", item.getProducto().getCategoria() != null ? item.getProducto().getCategoria() : "");
            m.put("cantidad", String.valueOf(item.getCantidad()));
            listaItems.add(m);
        }
    }
    resp.put("items", listaItems);
    
    // Boleta
    List<Boleta> boletas = boletaRepositorio.findAll().stream()
            .filter(b -> b.getReserva() != null && b.getReserva().getIdReserva().equals(idReserva))
            .toList();
    
    if (!boletas.isEmpty()) {
        Boleta b = boletas.get(0);
        resp.put("boletaId", b.getIdBoleta());
        resp.put("montoTotal", b.getTotal() != null ? b.getTotal().toString() : "0.00");
        resp.put("metodoPago", b.getTipoOperacion() != null ? b.getTipoOperacion() : "GRATUITO");
        resp.put("fechaEmision", b.getFechaEmision() != null ? b.getFechaEmision().toString() : "");
    } else {
        resp.put("boletaId", "—");
        resp.put("montoTotal", "0.00");
        resp.put("metodoPago", "GRATUITO");
        resp.put("fechaEmision", "—");
    }
    
    return resp;
}

@GetMapping("/api/estadisticas")
@org.springframework.web.bind.annotation.ResponseBody
@Transactional(readOnly = true)
public java.util.Map<String, Object> obtenerEstadisticas(@RequestParam(value = "mes", required = false) String mes, HttpSession sesion) {
    java.util.Map<String, Object> resp = new java.util.HashMap<>();
    
    Object usuario = sesion.getAttribute("usuarioLogueado");
    if (!(usuario instanceof Alumno)) return resp;
    
    Alumno alumno = (Alumno) usuario;
    List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
    
    // Determinar el mes y año a consultar
    java.time.LocalDate fechaConsulta;
    try {
        fechaConsulta = (mes != null && !mes.isEmpty()) ? java.time.LocalDate.parse(mes + "-01") : java.time.LocalDate.now();
    } catch (Exception e) {
        fechaConsulta = java.time.LocalDate.now();
    }
    
    int diasEnMes = fechaConsulta.lengthOfMonth();
    java.util.Map<Integer, Integer> reservasPorDia = new java.util.LinkedHashMap<>();
    java.util.Map<Integer, Integer> horasPorDia = new java.util.LinkedHashMap<>();
    java.util.Map<Integer, Integer> materialesPorDia = new java.util.LinkedHashMap<>();
    
    // Inicializar todos los días en 0
    for (int i = 1; i <= diasEnMes; i++) {
        reservasPorDia.put(i, 0);
        horasPorDia.put(i, 0);
        materialesPorDia.put(i, 0);
    }
    
    for (ReservaIntegrante ri : participaciones) {
        Reserva r = ri.getReserva();
        if (r == null || r.getFechaReserva() == null) continue;
        
        if (r.getFechaReserva().getMonth() == fechaConsulta.getMonth() && 
            r.getFechaReserva().getYear() == fechaConsulta.getYear()) {
            
            int dia = r.getFechaReserva().getDayOfMonth();
            reservasPorDia.put(dia, reservasPorDia.get(dia) + 1);
            
            if (r.getHoraInicio() != null && r.getHoraFin() != null) {
                long horas = java.time.Duration.between(r.getHoraInicio(), r.getHoraFin()).toHours();
                horasPorDia.put(dia, horasPorDia.get(dia) + (int) horas);
            }
            
            List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(r.getIdReserva());
            materialesPorDia.put(dia, materialesPorDia.get(dia) + items.size());
        }
    }
    
    resp.put("labels", new ArrayList<>(reservasPorDia.keySet())); // Días del 1 al 31
    resp.put("reservas", new ArrayList<>(reservasPorDia.values()));
    resp.put("horas", new ArrayList<>(horasPorDia.values()));
    resp.put("materiales", new ArrayList<>(materialesPorDia.values()));
    
    return resp;
}

@GetMapping("/api/incidencia/{idSancion}")
@org.springframework.web.bind.annotation.ResponseBody
@Transactional(readOnly = true)
public java.util.Map<String, Object> obtenerDetalleIncidencia(@PathVariable("idSancion") String idSancion) {
    java.util.Map<String, Object> resp = new java.util.HashMap<>();
    
    Optional<Sancion> opt = sancionRepositorio.findById(idSancion);
    if (opt.isEmpty()) {
        resp.put("encontrado", false);
        return resp;
    }
    
    Sancion s = opt.get();
    resp.put("encontrado", true);
    resp.put("idSancion", s.getIdSancion());
    resp.put("motivo", s.getMotivo());
    resp.put("monto", s.getMontoDeuda());
    resp.put("estado", s.getEstado());
    
    // Datos de la reserva y lab
    if (s.getReserva() != null) {
        resp.put("idReserva", s.getReserva().getIdReserva());
        resp.put("laboratorio", s.getReserva().getLaboratorio() != null ? s.getReserva().getLaboratorio().getNombre() : "Laboratorio");
        resp.put("cubiculo", s.getReserva().getLaboratorio() != null ? s.getReserva().getLaboratorio().getUbicacionCubiculo() : "S/C");
        resp.put("fechaReserva", s.getReserva().getFechaReserva() != null ? s.getReserva().getFechaReserva().toString() : "");
        resp.put("horario", s.getReserva().getHoraInicio() + " - " + s.getReserva().getHoraFin());
        
        // Integrantes
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(s.getReserva().getIdReserva());
        List<java.util.Map<String, String>> listaIntegrantes = new ArrayList<>();
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                java.util.Map<String, String> m = new java.util.HashMap<>();
                m.put("nombre", ri.getAlumno().getNombre() + " " + ri.getAlumno().getApellido());
                m.put("codigo", ri.getAlumno().getIdEstud());
                listaIntegrantes.add(m);
            }
        }
        resp.put("integrantes", listaIntegrantes);
    }
    
    // Datos de la boleta/pago (si ya está resuelta)
    List<Boleta> boletas = boletaRepositorio.findAll().stream()
            .filter(b -> b.getReserva() != null && b.getReserva().getIdReserva().equals(s.getReserva().getIdReserva()))
            .toList();
            
    if (!boletas.isEmpty()) {
        Boleta b = boletas.get(0);
        resp.put("montoPagado", b.getTotal());
        resp.put("metodoPago", b.getTipoOperacion());
        resp.put("fechaPago", b.getFechaEmision().toString());
    }
    
    return resp;
}

// =========================================================================
// DTO AUXILIAR PARA EL HISTORIAL DE RESERVAS
// =========================================================================
public static class HistorialReservaDto {
    private String idReserva;
    private String laboratorio;
    private String fecha;
    private String horario;
    private String estado;

    public HistorialReservaDto(String idReserva, String laboratorio, String fecha, String horario, String estado) {
        this.idReserva = idReserva;
        this.laboratorio = laboratorio;
        this.fecha = fecha;
        this.horario = horario;
        this.estado = estado;
    }

    public String getIdReserva() { return idReserva; }
    public String getLaboratorio() { return laboratorio; }
    public String getFecha() { return fecha; }
    public String getHorario() { return horario; }
    public String getEstado() { return estado; }
}
    // =========================================================================
    // DTO AUXILIAR PARA ENVÍO A THYMELEAF
    // =========================================================================
    public static class ReservaActivaDto {

        private String idReserva;
        private String laboratorio;
        private String cubiculo;
        private String fecha;
        private String horario;
        private String estado;

        public ReservaActivaDto(String idReserva, String laboratorio, String cubiculo, String fecha, String horario, String estado) {
            this.idReserva = idReserva;
            this.laboratorio = laboratorio;
            this.cubiculo = cubiculo;
            this.fecha = fecha;
            this.horario = horario;
            this.estado = estado;
        }

        public String getIdReserva() {
            return idReserva;
        }

        public String getLaboratorio() {
            return laboratorio;
        }

        public String getCubiculo() {
            return cubiculo;
        }

        public String getFecha() {
            return fecha;
        }

        public String getHorario() {
            return horario;
        }

        public String getEstado() {
            return estado;
        }
    }

    
@PostMapping("/reservas/{idReserva}/cancelar")
@Transactional
public String cancelarReserva(
        @PathVariable String idReserva,
        HttpSession sesion,
        RedirectAttributes redirectAttributes) {

    Object usuario = sesion.getAttribute("usuarioLogueado");

    if (!(usuario instanceof Alumno)) {
        return "redirect:/login";
    }

    Alumno alumno = (Alumno) usuario;

    Optional<Reserva> reservaOpt =
            reservaRepositorio.findById(idReserva);

    if (reservaOpt.isEmpty()) {
        redirectAttributes.addFlashAttribute(
                "mensajeCancelacion",
                "No se encontró la reserva."
        );
        return "redirect:/alumno/cuenta";
    }

    Reserva reserva = reservaOpt.get();

    // Comprobar que el alumno participa en esta reserva.
    List<ReservaIntegrante> integrantes =
            reservaIntegranteRepositorio
                    .findByReserva_IdReserva(idReserva);

    boolean participa = integrantes.stream()
            .anyMatch(ri -> ri.getAlumno() != null
                    && alumno.getIdEstud().equals(
                            ri.getAlumno().getIdEstud()));

    if (!participa) {
        redirectAttributes.addFlashAttribute(
                "mensajeCancelacion",
                "No tienes permiso para cancelar esta reserva."
        );
        return "redirect:/alumno/cuenta";
    }

    String estado = reserva.getEstado() == null
            ? ""
            : reserva.getEstado().trim().toUpperCase();

    if (!estado.equals("CONFIRMADA")) {
        redirectAttributes.addFlashAttribute(
                "mensajeCancelacion",
                "Solo se pueden cancelar reservas confirmadas."
        );
        return "redirect:/alumno/cuenta";
    }

    // Evitar cancelar una reserva cuya hora ya comenzó.
    // Solo se puede cancelar 30 minutos antes del inicio
    LocalDateTime inicioReserva = LocalDateTime.of(
            reserva.getFechaReserva(),
            reserva.getHoraInicio()
    );
    
    LocalDateTime limiteCancelacion = LocalDateTime.now().plusMinutes(30);

    if (inicioReserva.isBefore(limiteCancelacion)) {
        redirectAttributes.addFlashAttribute(
                "errorReserva",
                "No puedes cancelar la reserva porque faltan menos de 30 minutos para su inicio."
        );
        return "redirect:/alumno/laboratorios";
    }

    reserva.setEstado("CANCELADA");
    reservaRepositorio.save(reserva);

    redirectAttributes.addFlashAttribute(
            "mensajeCancelacion",
            "La reserva " + idReserva + " fue cancelada correctamente."
    );

    return "redirect:/alumno/cuenta";
}



}

