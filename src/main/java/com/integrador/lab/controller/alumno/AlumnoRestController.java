package com.integrador.lab.controller.alumno;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.repository.AlumnoRepository;

@RestController
@RequestMapping("/api/alumnos")
public class AlumnoRestController {

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @GetMapping("/buscar/{codigo}")
    public ResponseEntity<?> buscarPorCodigo(@PathVariable("codigo") String codigo) {
        String query = codigo.trim().toUpperCase();

        // 1. Busca por clave primaria (idEstud, ej. U24201349)
        Optional<Alumno> alumnoOpt = alumnoRepositorio.findById(query);

        // 2. Si no lo encuentra, busca por DNI
        if (alumnoOpt.isEmpty()) {
            alumnoOpt = alumnoRepositorio.findAll().stream()
                    .filter(a -> a.getDni() != null && a.getDni().trim().equalsIgnoreCase(query))
                    .findFirst();
        }

        if (alumnoOpt.isPresent()) {
            Alumno a = alumnoOpt.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("encontrado", true);
            resp.put("codigo", a.getIdEstud());
            resp.put("nombreCompleto", a.getNombre() + " " + a.getApellido());
            resp.put("habilitado", "HABILITADO".equalsIgnoreCase(a.getEstadoCuenta()) || a.getEstadoCuenta() == null);
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.ok(Map.of("encontrado", false));
    }
}

