package com.integrador.lab.controller.auth;

import com.integrador.lab.dto.CambioPasswordDTO;
import com.integrador.lab.exception.CambioPasswordException;
import com.integrador.lab.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mi-cuenta")
public class CambioPasswordController {

    private final UsuarioService usuarioService;

    public CambioPasswordController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 1. GET: Muestra el formulario
    @GetMapping("/cambiar-password")
    public String mostrarFormulario(HttpSession sesion, Model modelo) {
        
        // Verificamos que el alumno esté logueado
        String rol = (String) sesion.getAttribute("rol");
        if (rol == null || !rol.equals("ALUMNO")) {
            return "redirect:/login"; 
        }

        modelo.addAttribute("cambioPasswordDTO", new CambioPasswordDTO());
        return "auth/cambiar-password"; 
    }

    // 2. POST: Procesa el cambio
    @PostMapping("/cambiar-password")
    public String procesarCambio(@ModelAttribute CambioPasswordDTO dto,
                                 HttpSession sesion,
                                 RedirectAttributes redirectAttrs,
                                 Model modelo) {
        try {
            // Leemos el código del alumno de la sesión (así lo guardaste en LoginController)
            String codigoUtp = (String) sesion.getAttribute("codigoUtp");
            String rol = (String) sesion.getAttribute("rol");

            if (rol == null || !rol.equals("ALUMNO") || codigoUtp == null) {
                return "redirect:/login";
            }

            // Llamamos al método que creamos en el UsuarioService
            usuarioService.cambiarPasswordAlumno(codigoUtp, dto);

            // Si todo sale bien, mostramos mensaje de éxito
            redirectAttrs.addFlashAttribute("success", "¡Contraseña actualizada correctamente!");
            return "redirect:/mi-cuenta/cambiar-password"; // Recarga la misma página con el mensaje

        } catch (CambioPasswordException ex) {
            // Error de negocio (ej. "la nueva contraseña no puede ser igual")
            modelo.addAttribute("error", ex.getMessage());
            modelo.addAttribute("cambioPasswordDTO", dto); 
            return "auth/cambiar-password";
        } catch (Exception ex) {
            modelo.addAttribute("error", "Ocurrió un error inesperado. Intenta de nuevo.");
            return "auth/cambiar-password";
        }
    }
}
