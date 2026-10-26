package com.integrador.lab.controller.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.modelo.Laboratorio;
import com.integrador.lab.repository.AlumnoRepository;
import com.integrador.lab.service.UsuarioService;
import com.integrador.lab.service.UsuarioService.ResultadoLogin;
import com.integrador.lab.repository.LaboratorioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioServicio;

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @Autowired
    private LaboratorioRepository laboratorioRepositorio; 

    @GetMapping({"/", "/login"})
    public String mostrarLogin(HttpSession sesion) {
        Object usuarioActivo = sesion.getAttribute("usuarioLogueado");
        if (usuarioActivo != null) {
            String rol = (String) sesion.getAttribute("rol");
            return "ADMIN".equalsIgnoreCase(rol) ? "redirect:/admin/dashboard" : "redirect:/alumno/laboratorios";
        }
        return "lauth/ogin";
    }

     @PostMapping("/login")
    public String procesarLogin(@RequestParam("codigoUtp") String codigoUtp,
            @RequestParam("password") String password,
            HttpSession sesion,
            Model modelo) {
        try {
            ResultadoLogin resultado = usuarioServicio.validarCredenciales(codigoUtp, password);

            sesion.setAttribute("nombreUsuario", resultado.getNombreCompleto());
            sesion.setAttribute("codigoUtp", resultado.getCodigo());
            sesion.setAttribute("rol", resultado.getRol());

            if (resultado.esAdmin()) {
                sesion.setAttribute("usuarioLogueado", resultado);
                return "redirect:/admin/dashboard";
            } else {
                Alumno alumno = alumnoRepositorio.findById(resultado.getCodigo()).orElse(null);
                
                // ⭐ NUEVA VALIDACIÓN: Verificar si su carrera tiene laboratorios
                boolean carreraPermitida = false;
                if (alumno != null && alumno.getCarrera() != null) {
                    List<Laboratorio> labs = laboratorioRepositorio
                        .findLaboratoriosPermitidosPorCarrera(alumno.getCarrera().getIdCarrera());
                    carreraPermitida = !labs.isEmpty();
                }

                if (!carreraPermitida) {
                    // Si no tiene permiso, no lo dejamos entrar y mostramos error
                    modelo.addAttribute("error", "Tu carrera no cuenta con acceso a los laboratorios de la universidad.");
                    return "auth/login";
                }

                // Si todo está bien, entra al sistema
                sesion.setAttribute("usuarioLogueado", alumno);
                return "redirect:/alumno/laboratorios";
            }

        } catch (RuntimeException excepcion) {
            modelo.addAttribute("error", excepcion.getMessage());
            modelo.addAttribute("codigoIngresado", codigoUtp);
            return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/alumno/panel")
    public String panelAlumno(HttpSession sesion) {
        return "redirect:/alumno/laboratorios";
    }
}

