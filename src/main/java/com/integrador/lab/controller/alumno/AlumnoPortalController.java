package com.integrador.lab.controller.alumno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import com.integrador.lab.service.EmailService;

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

    @Autowired 
    private EmailService emailService;

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
        boolean estaBloqueado = incidenciasPendientes > 0 || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta);

        // 2. Obtener la carrera y validar si permite laboratorios
        String idCarrera = (alumno.getCarrera() != null) ? alumno.getCarrera().getIdCarrera() : null;
        
        List<Laboratorio> labs = new ArrayList<>();
        boolean puedeReservarPorCarrera = false;

        if (idCarrera != null) {
            labs = laboratorioRepositorio.findLaboratoriosPermitidosPorCarrera(idCarrera);
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

        // 4. Contar reservas del día (TODAS MENOS CANCELADAS)
        List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
        LocalDate hoy = LocalDate.now();
        List<String> idsReservasHoy = new ArrayList<>();

        for (ReservaIntegrante ri : participaciones) {
            Reserva r = ri.getReserva();
            if (r == null || r.getFechaReserva() == null || !hoy.equals(r.getFechaReserva())) continue;

            String estado = r.getEstado() == null ? "" : r.getEstado().trim().toUpperCase();
            
            if (!estado.equals("CANCELADA")) {
                if (!idsReservasHoy.contains(r.getIdReserva())) {
                    idsReservasHoy.add(r.getIdReserva());
                }
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

        return "alumno/laboratorios";
    }

    // =========================================================================
    // 2. VISTA INDEPENDIENTE: CATÁLOGO DE INSUMOS
    // =========================================================================
    @GetMapping("/catalogos")
    @Transactional(readOnly = true)
    public String verCatalogo(HttpSession sesion, Model modelo) {
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        Alumno alumno = null;
        if (usuario instanceof Alumno) {
            alumno = (Alumno) usuario;
        } else {
            return "redirect:/login";
        }

        List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
        long incidenciasPendientes = sancionesPendientes.size();

        String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
        boolean estaBloqueado = incidenciasPendientes > 0 || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta) || estadoCuenta.contains("BLOQUEADO_POR_DEUDA");

        List<Producto> todosLosProductos = productoRepositorio.findAll();

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

        return "alumno/catalogos";
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
        if (usuario == null) return "redirect:/login";
        Alumno alumnoLogueado = (Alumno) usuario;

        String idCarreraAlumno = (alumnoLogueado.getCarrera() != null) ? alumnoLogueado.getCarrera().getIdCarrera() : null;
        if (idCarreraAlumno == null) return "redirect:/alumno/laboratorios";
        
        List<Laboratorio> labsPermitidos = laboratorioRepositorio.findLaboratoriosPermitidosPorCarrera(idCarreraAlumno);
        boolean labPermitido = labsPermitidos.stream().anyMatch(l -> l.getIdLab().equals(idLab));
        if (!labPermitido) {
            redirectAttributes.addFlashAttribute("errorReserva", "Tu carrera no tiene acceso a este laboratorio.");
            return "redirect:/alumno/laboratorios";
        }

        boolean tieneSancion = sancionRepositorio.existsByAlumno_IdEstudAndEstado(alumnoLogueado.getIdEstud(), "PENDIENTE");
        String estadoCuentaAlumno = (alumnoLogueado.getEstadoCuenta() != null) ? alumnoLogueado.getEstadoCuenta().trim().toUpperCase() : "";
        boolean cuentaBloqueada = "BLOQUEADO_POR_DEUDA".equals(estadoCuentaAlumno) || estadoCuentaAlumno.contains("BLOQUEADO");

        if (tieneSancion || cuentaBloqueada) return "redirect:/alumno/laboratorios";

        Optional<Laboratorio> labOpt = laboratorioRepositorio.findById(idLab);
        if (labOpt.isEmpty()) return "redirect:/alumno/laboratorios";
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

        if (fecha.isBefore(LocalDate.now())) return "redirect:/alumno/laboratorios";
        if (!horaFin.isAfter(horaInicio)) return "redirect:/alumno/laboratorios";

        // 1. LÍMITE DE DOS RESERVAS DIARIAS (TODAS MENOS CANCELADAS)
        List<ReservaIntegrante> participacionesAlumno = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumnoLogueado.getIdEstud());
        List<String> idsReservasDelDia = new ArrayList<>();

        for (ReservaIntegrante participacion : participacionesAlumno) {
            Reserva reservaExistente = participacion.getReserva();
            if (reservaExistente == null || reservaExistente.getFechaReserva() == null || !fecha.equals(reservaExistente.getFechaReserva())) continue;
            
            String estado = reservaExistente.getEstado() == null ? "" : reservaExistente.getEstado().trim().toUpperCase();
            
            if (!estado.equals("CANCELADA")) {
                if (!idsReservasDelDia.contains(reservaExistente.getIdReserva())) {
                    idsReservasDelDia.add(reservaExistente.getIdReserva());
                }
            }
        }

        if (idsReservasDelDia.size() >= 2) {
            redirectAttributes.addFlashAttribute("errorReserva", "Ya alcanzaste el límite de dos reservas para este día. Las reservas canceladas no cuentan.");
            return "redirect:/alumno/laboratorios";
        }

        // B. Validar integrantes
        if (codigosIntegrantes != null) {
            for (String codIntegrante : codigosIntegrantes) {
                if (codIntegrante.trim().equalsIgnoreCase(alumnoLogueado.getIdEstud().trim())) continue;

                List<ReservaIntegrante> participacionesIntegrante = reservaIntegranteRepositorio.findByAlumno_IdEstud(codIntegrante.trim());
                List<String> idsReservasIntegrante = new ArrayList<>();

                for (ReservaIntegrante part : participacionesIntegrante) {
                    Reserva r = part.getReserva();
                    if (r == null || r.getFechaReserva() == null || !fecha.equals(r.getFechaReserva())) continue;

                    String est = r.getEstado() == null ? "" : r.getEstado().trim().toUpperCase();
                    if (!est.equals("CANCELADA")) {
                        if (!idsReservasIntegrante.contains(r.getIdReserva())) {
                            idsReservasIntegrante.add(r.getIdReserva());
                        }
                    }
                }

                if (idsReservasIntegrante.size() >= 2) {
                    redirectAttributes.addFlashAttribute("errorReserva", 
                        "No se puede completar la reserva. El alumno con código " + codIntegrante + " ya alcanzó su límite de 2 reservas para este día.");
                    return "redirect:/alumno/laboratorios";
                }
            }
        }

        // 2. EVITAR SUPERPOSICIONES
        List<Reserva> reservasDelLaboratorio = reservaRepositorio.findByLaboratorio_IdLabAndFechaReserva(idLab, fecha);

        for (Reserva reservaExistente : reservasDelLaboratorio) {
            String estado = reservaExistente.getEstado() == null ? "" : reservaExistente.getEstado().trim().toUpperCase();
            boolean reservaActiva = estado.equals("CONFIRMADA") || estado.equals("EN_USO");
            if (!reservaActiva) continue;

            LocalTime inicioExistente = reservaExistente.getHoraInicio();
            LocalTime finExistente = reservaExistente.getHoraFin();
            if (inicioExistente == null || finExistente == null) continue;

            boolean haySuperposicion = horaInicio.isBefore(finExistente) && horaFin.isAfter(inicioExistente);

            if (haySuperposicion) {
                redirectAttributes.addFlashAttribute("errorReserva", "¡Cruce de horario! El laboratorio ya tiene una reserva activa durante el horario seleccionado. Elige otra hora.");
                return "redirect:/alumno/laboratorios";
            }
        }

        // A. Generación de ID
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

        // B. Guardar Responsable
        ReservaIntegrante responsable = new ReservaIntegrante(nuevaReserva, alumnoLogueado, false);
        reservaIntegranteRepositorio.save(responsable);

        // C. Guardar Integrantes
        if (codigosIntegrantes != null) {
            for (String cod : codigosIntegrantes) {
                if (!cod.trim().equalsIgnoreCase(alumnoLogueado.getIdEstud().trim())) {
                    Alumno integrante = alumnoRepositorio.findById(cod.trim()).orElse(null);
                    
                    if (integrante != null && integrante.getCarrera() != null) {
                        List<Laboratorio> labsPermitidosIntegrante = laboratorioRepositorio.findLaboratoriosPermitidosPorCarrera(integrante.getCarrera().getIdCarrera());
                        
                        if (labsPermitidosIntegrante.isEmpty()) {
                            redirectAttributes.addFlashAttribute("errorReserva", "El alumno con código " + cod + " pertenece a una carrera que no tiene acceso a laboratorios.");
                            return "redirect:/alumno/laboratorios";
                        }
                    }

                    if (integrante != null) {
                        reservaIntegranteRepositorio.save(new ReservaIntegrante(nuevaReserva, integrante, false));
                    }
                }
            }
        }

        // D. Guardar Insumos y EPPs
        List<String> productosElegidos = new ArrayList<>();
        if (insumosIds != null) productosElegidos.addAll(insumosIds);
        if (eppsIds != null) productosElegidos.addAll(eppsIds);

        for (String idProd : productosElegidos) {
            productoRepositorio.findById(idProd).ifPresent(prod -> {
                ReservaItem item = new ReservaItem(nuevaReserva, prod, 1, false);
                reservaItemRepositorio.save(item);

                if (prod.getStockActual() != null && prod.getStockActual() > 0) {
                    prod.setStockActual(prod.getStockActual() - 1);
                    productoRepositorio.save(prod);
                }
            });
        }

        // E. Boleta
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

        // ⭐ ENVIAR CORREO DE CONFIRMACIÓN
        try {
            String correoDestino = alumnoLogueado.getIdEstud().toLowerCase() + "@utp.edu.pe";
            String asunto = "✅ Confirmación de Reserva - UTP +Lab";
            String mensaje = "Hola " + alumnoLogueado.getNombre() + ",\n\n" +
                    "Tu reserva en " + lab.getNombre() + " (" + lab.getUbicacionCubiculo() + ") ha sido confirmada.\n" +
                    "Fecha: " + fecha + "\n" +
                    "Horario: " + horaInicio + " - " + horaFin + "\n\n" +
                    "Recuerda llegar 10 minutos antes para tu check-in.\n\n" +
                    "Atentamente,\nEl equipo de UTP +Lab";

            emailService.enviarCorreo(correoDestino, asunto, mensaje);
        } catch (Exception e) {
            System.out.println("❌ Error al enviar correo: " + e.getMessage());
        }

        return "redirect:/alumno/laboratorios";
    }

    // =========================================================================
    // 4. VISTA INDEPENDIENTE: PROTOCOLOS DE SEGURIDAD
    // =========================================================================
    @GetMapping("/protocolos")
    @Transactional(readOnly = true)
    public String verProtocolos(HttpSession sesion, Model modelo) {
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        Alumno alumno = null;
        if (usuario instanceof Alumno) {
            alumno = (Alumno) usuario;
        } else {
            return "redirect:/login";
        }

        List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
        long incidenciasPendientes = sancionesPendientes.size();

        String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
        boolean estaBloqueado = incidenciasPendientes > 0 || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta);

        modelo.addAttribute("alumno", alumno);
        modelo.addAttribute("estaBloqueado", estaBloqueado);
        modelo.addAttribute("numIncidencias", incidenciasPendientes);

        return "alumno/protocolos";
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

        // FECHA DE CONSULTA
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
        
        String mesInput = String.format("%04d-%02d", fechaConsulta.getYear(), fechaConsulta.getMonthValue());

        // SANCIONES
        List<Sancion> sancionesPendientes = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE");
        long incidenciasPendientes = sancionesPendientes.size();

        String estadoCuenta = (alumno.getEstadoCuenta() != null) ? alumno.getEstadoCuenta().trim().toUpperCase() : "";
        boolean estaBloqueado = incidenciasPendientes > 0 || "BLOQUEADO_POR_DEUDA".equals(estadoCuenta);

        // HISTORIAL Y ESTADÍSTICAS
        List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
        List<HistorialReservaDto> historial = new ArrayList<>();
        
        int reservasDelMes = 0;
        int horasTotales = 0;
        int materialesTotales = 0;

        for (ReservaIntegrante ri : participaciones) {
            Reserva r = ri.getReserva();
            if (r == null || r.getFechaReserva() == null) continue;

            String dia = String.valueOf(r.getFechaReserva().getDayOfMonth());
            String mesAbrev = r.getFechaReserva().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, espLocale);
            mesAbrev = mesAbrev.substring(0, 1).toUpperCase() + mesAbrev.substring(1).replace(".", "");
            String fechaFormato = dia + " " + mesAbrev + " " + r.getFechaReserva().getYear();
            
            String horario = "";
            if (r.getHoraInicio() != null && r.getHoraFin() != null) {
                horario = r.getHoraInicio() + "–" + r.getHoraFin();
            }

            boolean esDelMes = r.getFechaReserva().getMonthValue() == fechaConsulta.getMonthValue() 
                            && r.getFechaReserva().getYear() == fechaConsulta.getYear();

            if (esDelMes) {
                reservasDelMes++;
                if (r.getHoraInicio() != null && r.getHoraFin() != null) {
                    long horas = java.time.Duration.between(r.getHoraInicio(), r.getHoraFin()).toHours();
                    horasTotales += (int) horas;
                }
                List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(r.getIdReserva());
                materialesTotales += items.size();

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

        historial.sort((a, b) -> b.getIdReserva().compareTo(a.getIdReserva()));

        modelo.addAttribute("alumno", alumno);
        modelo.addAttribute("estaBloqueado", estaBloqueado);
        modelo.addAttribute("numIncidencias", incidenciasPendientes);
        modelo.addAttribute("reservasDelMes", reservasDelMes);
        modelo.addAttribute("horasTotales", horasTotales);
        modelo.addAttribute("materialesTotales", materialesTotales);
        modelo.addAttribute("historial", historial);
        
        modelo.addAttribute("mesFormateado", mesFormateado);
        modelo.addAttribute("mesInput", mesInput);
        modelo.addAttribute("mesAnterior", String.format("%04d-%02d", mesAnterior.getYear(), mesAnterior.getMonthValue()));
        modelo.addAttribute("mesSiguiente", String.format("%04d-%02d", mesSiguiente.getYear(), mesSiguiente.getMonthValue()));
        modelo.addAttribute("esMesActual", fechaConsulta.getMonthValue() == hoy.getMonthValue() && fechaConsulta.getYear() == hoy.getYear());

                
        return "alumno/cuenta";
    }

    // =========================================================================
    // 6. PAGAR INCIDENCIA (ALUMNO)
    // =========================================================================
    @GetMapping("/incidencias/pagar/{idSancion}")
    @Transactional
    public String pagarIncidencia(
            @PathVariable("idSancion") String idSancion, 
            HttpSession sesion, 
            RedirectAttributes redirectAttributes) {
        
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";
        Alumno alumno = (Alumno) usuario;

        Optional<Sancion> opt = sancionRepositorio.findById(idSancion);
        
        if (opt.isPresent()) {
            Sancion s = opt.get();
            
            // Verificar que la sanción pertenezca al alumno logueado
            if (s.getAlumno() == null || !s.getAlumno().getIdEstud().equals(alumno.getIdEstud())) {
                redirectAttributes.addFlashAttribute("mensajeCancelacion", "No tienes permiso para pagar esta incidencia.");
                return "redirect:/alumno/cuenta";
            }

            // Cambiar estado a PAGADA
            s.setEstado("PAGADA");
            sancionRepositorio.save(s);

            // Verificar si el alumno ya no tiene más deudas para desbloquearlo
            long pendientes = sancionRepositorio.countByAlumno_IdEstudAndEstadoNotIn(
                alumno.getIdEstud(), 
                List.of("PAGADA", "RESUELTA")
            );

            if (pendientes == 0) {
                Alumno al = alumnoRepositorio.findById(alumno.getIdEstud()).orElse(null);
                if (al != null) {
                    al.setEstadoCuenta("HABILITADO");
                    alumnoRepositorio.save(al);
                }
            }
            
            redirectAttributes.addFlashAttribute("mensajeCancelacion", "✅ El pago de la incidencia " + idSancion + " se registró correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeCancelacion", "No se encontró la incidencia.");
        }
        
        return "redirect:/alumno/cuenta";
    }

    // =========================================================================
    // 7. APIs
    // =========================================================================
    @GetMapping("/api/recibo/{idReserva}")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerRecibo(@PathVariable("idReserva") String idReserva) {
        Map<String, Object> resp = new HashMap<>();
        
        Optional<Reserva> reservaOpt = reservaRepositorio.findById(idReserva);
        if (reservaOpt.isEmpty()) {
            resp.put("encontrado", false);
            return resp;
        }
        
        Reserva r = reservaOpt.get();
        
        resp.put("encontrado", true);
        resp.put("idReserva", r.getIdReserva());
        resp.put("laboratorio", r.getLaboratorio() != null ? r.getLaboratorio().getNombre() : "Laboratorio");
        resp.put("cubiculo", r.getLaboratorio() != null ? r.getLaboratorio().getUbicacionCubiculo() : "S/C");
        resp.put("fecha", r.getFechaReserva() != null ? r.getFechaReserva().toString() : "");
        resp.put("horaInicio", r.getHoraInicio() != null ? r.getHoraInicio().toString() : "");
        resp.put("horaFin", r.getHoraFin() != null ? r.getHoraFin().toString() : "");
        resp.put("estado", r.getEstado());
        
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);
        List<Map<String, String>> listaIntegrantes = new ArrayList<>();
        for (ReservaIntegrante ri : integrantes) {
            if (ri.getAlumno() != null) {
                Map<String, String> m = new HashMap<>();
                m.put("nombre", ri.getAlumno().getNombre() + " " + ri.getAlumno().getApellido());
                m.put("codigo", ri.getAlumno().getIdEstud());
                listaIntegrantes.add(m);
            }
        }
        resp.put("integrantes", listaIntegrantes);
        
        List<ReservaItem> items = reservaItemRepositorio.findByReserva_IdReserva(idReserva);
        List<Map<String, String>> listaItems = new ArrayList<>();
        for (ReservaItem item : items) {
            if (item.getProducto() != null) {
                Map<String, String> m = new HashMap<>();
                m.put("nombre", item.getProducto().getNombre());
                m.put("categoria", item.getProducto().getCategoria() != null ? item.getProducto().getCategoria() : "");
                m.put("cantidad", String.valueOf(item.getCantidad()));
                listaItems.add(m);
            }
        }
        resp.put("items", listaItems);
        
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
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(@RequestParam(value = "mes", required = false) String mes, HttpSession sesion) {
        Map<String, Object> resp = new HashMap<>();
        
        Object usuario = sesion.getAttribute("usuarioLogueado");
        if (!(usuario instanceof Alumno)) return resp;
        
        Alumno alumno = (Alumno) usuario;
        List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
        
        LocalDate fechaConsulta;
        try {
            fechaConsulta = (mes != null && !mes.isEmpty()) ? LocalDate.parse(mes + "-01") : LocalDate.now();
        } catch (Exception e) {
            fechaConsulta = LocalDate.now();
        }
        
        int diasEnMes = fechaConsulta.lengthOfMonth();
        Map<Integer, Integer> reservasPorDia = new java.util.LinkedHashMap<>();
        Map<Integer, Integer> horasPorDia = new java.util.LinkedHashMap<>();
        Map<Integer, Integer> materialesPorDia = new java.util.LinkedHashMap<>();
        
        for (int i = 1; i <= diasEnMes; i++) {
            reservasPorDia.put(i, 0);
            horasPorDia.put(i, 0);
            materialesPorDia.put(i, 0);
        }
        
        for (ReservaIntegrante ri : participaciones) {
            Reserva r = ri.getReserva();
            if (r == null || r.getFechaReserva() == null) continue;
            
            if (r.getFechaReserva().getMonthValue() == fechaConsulta.getMonthValue() && 
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
        
        resp.put("labels", new ArrayList<>(reservasPorDia.keySet()));
        resp.put("reservas", new ArrayList<>(reservasPorDia.values()));
        resp.put("horas", new ArrayList<>(horasPorDia.values()));
        resp.put("materiales", new ArrayList<>(materialesPorDia.values()));
        
        return resp;
    }

    @GetMapping("/api/incidencia/{idSancion}")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleIncidencia(@PathVariable("idSancion") String idSancion) {
        Map<String, Object> resp = new HashMap<>();
        
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
        
        if (s.getReserva() != null) {
            resp.put("idReserva", s.getReserva().getIdReserva());
            resp.put("laboratorio", s.getReserva().getLaboratorio() != null ? s.getReserva().getLaboratorio().getNombre() : "Laboratorio");
            resp.put("cubiculo", s.getReserva().getLaboratorio() != null ? s.getReserva().getLaboratorio().getUbicacionCubiculo() : "S/C");
            resp.put("fechaReserva", s.getReserva().getFechaReserva() != null ? s.getReserva().getFechaReserva().toString() : "");
            resp.put("horario", s.getReserva().getHoraInicio() + " - " + s.getReserva().getHoraFin());
            
            List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(s.getReserva().getIdReserva());
            List<Map<String, String>> listaIntegrantes = new ArrayList<>();
            for (ReservaIntegrante ri : integrantes) {
                if (ri.getAlumno() != null) {
                    Map<String, String> m = new HashMap<>();
                    m.put("nombre", ri.getAlumno().getNombre() + " " + ri.getAlumno().getApellido());
                    m.put("codigo", ri.getAlumno().getIdEstud());
                    listaIntegrantes.add(m);
                }
            }
            resp.put("integrantes", listaIntegrantes);
        }
        
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
    // 8. CANCELAR RESERVA
    // =========================================================================
    @PostMapping("/reservas/{idReserva}/cancelar")
    @Transactional
    public String cancelarReserva(
            @PathVariable String idReserva,
            HttpSession sesion,
            RedirectAttributes redirectAttributes) {

        Object usuario = sesion.getAttribute("usuarioLogueado");

        if (!(usuario instanceof Alumno)) return "redirect:/login";

        Alumno alumno = (Alumno) usuario;
        Optional<Reserva> reservaOpt = reservaRepositorio.findById(idReserva);

        if (reservaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeCancelacion", "No se encontró la reserva.");
            return "redirect:/alumno/cuenta";
        }

        Reserva reserva = reservaOpt.get();
        List<ReservaIntegrante> integrantes = reservaIntegranteRepositorio.findByReserva_IdReserva(idReserva);

        boolean participa = integrantes.stream()
                .anyMatch(ri -> ri.getAlumno() != null && alumno.getIdEstud().equals(ri.getAlumno().getIdEstud()));

        if (!participa) {
            redirectAttributes.addFlashAttribute("mensajeCancelacion", "No tienes permiso para cancelar esta reserva.");
            return "redirect:/alumno/cuenta";
        }

        String estado = reserva.getEstado() == null ? "" : reserva.getEstado().trim().toUpperCase();

        if (!estado.equals("CONFIRMADA")) {
            redirectAttributes.addFlashAttribute("mensajeCancelacion", "Solo se pueden cancelar reservas confirmadas.");
            return "redirect:/alumno/cuenta";
        }

        // ⭐ ZONA HORARIA DE PERÚ
        java.time.ZoneId zonaPeru = java.time.ZoneId.of("America/Lima");
        LocalDateTime ahoraPeru = LocalDateTime.now(zonaPeru);

        LocalDateTime inicioReserva = LocalDateTime.of(reserva.getFechaReserva(), reserva.getHoraInicio());
        LocalDateTime limiteCancelacion = ahoraPeru.plusMinutes(30);

        if (inicioReserva.isBefore(limiteCancelacion)) {
            redirectAttributes.addFlashAttribute("errorReserva", "No puedes cancelar la reserva porque faltan menos de 30 minutos para su inicio.");
            return "redirect:/alumno/laboratorios";
        }

        reserva.setEstado("CANCELADA");
        reservaRepositorio.save(reserva);

        redirectAttributes.addFlashAttribute("mensajeCancelacion", "La reserva " + idReserva + " fue cancelada correctamente.");
        return "redirect:/alumno/cuenta";
    }

    @GetMapping("/api/notificaciones")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Map<String, String>> obtenerNotificaciones(HttpSession sesion) {
        List<Map<String, String>> notificaciones = new ArrayList<>();
        Object usuario = sesion.getAttribute("usuarioLogueado");
        
        if (!(usuario instanceof Alumno)) return notificaciones;
        Alumno alumno = (Alumno) usuario;

        long incidencias = sancionRepositorio.findByAlumno_IdEstudAndEstado(alumno.getIdEstud(), "PENDIENTE").size();
        if (incidencias > 0) {
            Map<String, String> notif = new HashMap<>();
            notif.put("titulo", "Tienes incidencias pendientes");
            notif.put("mensaje", "Tienes " + incidencias + " incidencia(s) sin resolver. Ve a la sección Incidencias.");
            notif.put("url", "/alumno/incidencias");
            notificaciones.add(notif);
        }

        List<ReservaIntegrante> participaciones = reservaIntegranteRepositorio.findByAlumno_IdEstud(alumno.getIdEstud());
        LocalDateTime ahora = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

        for (ReservaIntegrante ri : participaciones) {
            Reserva r = ri.getReserva();
            if (r == null || r.getFechaReserva() == null || !"CONFIRMADA".equals(r.getEstado())) continue;

            LocalDateTime inicio = LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio());
            long minutosFaltantes = java.time.Duration.between(ahora, inicio).toMinutes();

            if (minutosFaltantes > 0 && minutosFaltantes <= 60) {
                Map<String, String> notif = new HashMap<>();
                notif.put("titulo", "¡Reserva próxima!");
                notif.put("mensaje", "Tu reserva en " + r.getLaboratorio().getNombre() + " empieza en " + minutosFaltantes + " minutos.");
                notif.put("url", "/alumno/laboratorios");
                notificaciones.add(notif);
            }
        }

        return notificaciones;
    }

    // =========================================================================
    // DTOs AUXILIARES
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

        public String getIdReserva() { return idReserva; }
        public String getLaboratorio() { return laboratorio; }
        public String getCubiculo() { return cubiculo; }
        public String getFecha() { return fecha; }
        public String getHorario() { return horario; }
        public String getEstado() { return estado; }
    }
}