package com.integrador.lab.config;

import com.integrador.lab.repository.SancionRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private SancionRepository sancionRepositorio;

    /**
     * Agrega el contador de incidencias ACTIVAS (no resueltas) a TODOS los modelos.
     * Disponible en cualquier template como ${totalIncidencias}.
     */
    @ModelAttribute("totalIncidencias")
    public long getTotalIncidencias() {
        try {
            // Contamos solo las que no están resueltas ni pagadas
            return sancionRepositorio.findAll().stream()
                    .filter(s -> !"RESUELTA".equalsIgnoreCase(s.getEstado()) 
                              && !"PAGADA".equalsIgnoreCase(s.getEstado()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("requestURI")
    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}