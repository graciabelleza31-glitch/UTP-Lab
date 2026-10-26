package com.integrador.lab.controller.auth;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentoController {

    @GetMapping("/docs/terminos.pdf")
    public ResponseEntity<Resource> descargarTerminos() {
        try {
            // Busca el archivo en src/main/resources/static/docs/terminos.pdf
            Resource resource = new ClassPathResource("static/docs/terminos.pdf");
            
            if (!resource.exists()) {
                System.out.println("❌ El archivo terminos.pdf NO EXISTE en static/docs/");
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"terminos.pdf\"")
                    .body(resource);

        } catch (Exception e) {
            System.out.println("❌ Error al leer el PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

