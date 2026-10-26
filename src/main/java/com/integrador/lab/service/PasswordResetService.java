package com.integrador.lab.service;

import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.modelo.PasswordResetToken;
import com.integrador.lab.repository.AlumnoRepository;
import com.integrador.lab.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @Autowired
    private PasswordResetTokenRepository tokenRepositorio;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder; 

    /**
     * Paso 1: Genera un código, lo guarda en la BD y lo envía por correo.
     */
    @Transactional
    public String generarYEnviarCodigo(String idEstud) {
        Optional<Alumno> alumnoOpt = alumnoRepositorio.findById(idEstud.trim().toUpperCase());
        
        if (alumnoOpt.isEmpty()) {
            return null;
        }
        
        Alumno alumno = alumnoOpt.get();
        
        String codigo = String.format("%06d", new Random().nextInt(999999));
        String idToken = "TKN" + (System.currentTimeMillis() % 10000000);
        
        PasswordResetToken token = new PasswordResetToken(
                idToken,
                alumno,
                codigo,
                LocalDateTime.now().plusMinutes(15),
                false
        );
        
        tokenRepositorio.save(token);
        
        String correoDestino = alumno.getIdEstud().toLowerCase() + "@utp.edu.pe";
        
        emailService.enviarCodigoRestablecimiento(
                correoDestino,
                alumno.getNombre(),
                codigo
        );
        
        return enmascararCorreo(correoDestino);
    }

    /**
     * Paso 2: Verifica el código y devuelve el token si es válido.
     */
    @Transactional(readOnly = true)
    public PasswordResetToken verificarCodigo(String codigo) {
        Optional<PasswordResetToken> tokenOpt = tokenRepositorio.findByCodigoTemporalAndUsadoFalse(codigo.trim());
        
        if (tokenOpt.isEmpty()) {
            return null;
        }
        
        PasswordResetToken token = tokenOpt.get();
        
        if (token.getFechaVencimiento().isBefore(LocalDateTime.now())) {
            return null;
        }
        
        return token;
    }

    /**
     * Paso 3: Cambia la contraseña del alumno con validación doble (Opción B)
     */
    @Transactional
    public boolean cambiarPassword(String idToken, String nuevaPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepositorio.findById(idToken);
        
        if (tokenOpt.isEmpty()) {
            System.out.println("❌ Servicio: Token no encontrado en la BD con id: " + idToken);
            return false;
        }
        
        PasswordResetToken token = tokenOpt.get();
        
        if (token.getUsado() || token.getFechaVencimiento().isBefore(LocalDateTime.now())) {
            System.out.println("❌ Servicio: Token usado o expirado");
            return false;
        }
        
        Alumno alumno = token.getAlumno();
        String passwordBD = alumno.getPassword();

        System.out.println("=== PRUEBA DE CONTRASEÑA (OPCIÓN B) ===");
        System.out.println("Nueva ingresada: [" + nuevaPassword + "]");
        System.out.println("En la BD:        [" + passwordBD + "]");

        // 1. Comparamos si es igual en TEXTO PLANO
        boolean esIgualTextoPlano = nuevaPassword.equals(passwordBD);
        
        // 2. Comparamos si es igual ENCRIPTADO (por si en Render está con hash)
        boolean esIgualEncriptado = false;
        try {
            esIgualEncriptado = passwordEncoder.matches(nuevaPassword, passwordBD);
        } catch (Exception e) {
            // Si falla el matches (porque no es un hash válido), ignoramos el error
        }

        // 3. Si es igual por CUALQUIERA de las dos vías, lanzamos el error
        if (esIgualTextoPlano || esIgualEncriptado) {
            System.out.println("🛑 Servicio: ¡SON IGUALES! Lanzando error.");
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la anterior.");
        }

        // 4. Guardamos la nueva contraseña en texto plano (para tu presentación)
        System.out.println("✅ Servicio: Contraseña cambiada con éxito.");
        alumno.setPassword(nuevaPassword); 
        alumnoRepositorio.save(alumno);
        
        token.setUsado(true);
        tokenRepositorio.save(token);
        
        return true;
    }

    /**
     * Enmascara un correo: "u24201349@utp.edu.pe" → "u24*****@utp.edu.pe"
     */
    private String enmascararCorreo(String correo) {
        int arroba = correo.indexOf("@");
        if (arroba <= 3) return correo;
        
        String usuario = correo.substring(0, arroba);
        String dominio = correo.substring(arroba);
        
        String visible = usuario.substring(0, 3);
        String oculto = "*".repeat(Math.max(0, usuario.length() - 3));
        
        return visible + oculto + dominio;
    }
}
