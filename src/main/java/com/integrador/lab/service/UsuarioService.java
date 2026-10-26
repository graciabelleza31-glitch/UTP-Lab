package com.integrador.lab.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.integrador.lab.dto.CambioPasswordDTO;
import com.integrador.lab.exception.CambioPasswordException;
import com.integrador.lab.modelo.Administrador;
import com.integrador.lab.modelo.Alumno;
import com.integrador.lab.repository.AdministradorRepository;
import com.integrador.lab.repository.AlumnoRepository;

@Service
public class UsuarioService {

    @Autowired
    private AlumnoRepository alumnoRepositorio;

    @Autowired
    private AdministradorRepository administradorRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResultadoLogin validarCredenciales(String identificador, String password) {
        if (identificador == null || identificador.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Por favor, ingresa tu usuario y contraseña.");
        }

        String entrada = identificador.trim();
        String clave = password.trim();

        Optional<Alumno> alumnoOpcional = alumnoRepositorio.findById(entrada.toUpperCase());
        if (alumnoOpcional.isEmpty()) {
            alumnoOpcional = alumnoRepositorio.findByDni(entrada);
        }

        if (alumnoOpcional.isPresent()) {
            Alumno alumno = alumnoOpcional.get();

            // LÓGICA DE LOGIN HÍBRIDA (Alumno)
            String passwordBD = alumno.getPassword();
            boolean loginValido = false;
            
            if (clave.equals(passwordBD)) {
                loginValido = true;
            } else {
                try {
                    if (passwordEncoder.matches(clave, passwordBD)) {
                        loginValido = true;
                    }
                } catch (Exception e) {
                    // Ignorar si el hash es inválido
                }
            }

            if (!loginValido) {
                throw new RuntimeException("Código UTP o contraseña incorrectos.");
            }

            if (Boolean.FALSE.equals(alumno.getEsMatriculado())) {
                throw new RuntimeException("El estudiante no figura como matriculado en el ciclo actual.");
            }

            String nombreCarrera = (alumno.getCarrera() != null)
                    ? alumno.getCarrera().getNombre()
                    : "General";

            return new ResultadoLogin(
                    "ALUMNO",
                    alumno.getIdEstud(),
                    alumno.getNombre(),
                    alumno.getApellido(),
                    alumno.getEstadoCuenta(),
                    nombreCarrera
            );
        }

        Optional<Administrador> adminOpcional = administradorRepositorio.findById(entrada.toUpperCase());
        if (adminOpcional.isEmpty()) {
            adminOpcional = administradorRepositorio.findByDni(entrada);
        }

        if (adminOpcional.isPresent()) {
            Administrador admin = adminOpcional.get();

            // LÓGICA DE LOGIN HÍBRIDA (Admin)
            String passwordBD = admin.getPassword();
            boolean loginValido = false;
            
            if (clave.equals(passwordBD)) {
                loginValido = true;
            } else {
                try {
                    if (passwordEncoder.matches(clave, passwordBD)) {
                        loginValido = true;
                    }
                } catch (Exception e) {
                     // Ignorar
                }
            }

            if (!loginValido) {
                throw new RuntimeException("Código o contraseña incorrectos.");
            }

            return new ResultadoLogin(
                    "ADMIN",
                    admin.getIdAdmin(),
                    admin.getNombre(),
                    admin.getApellido(),
                    "HABILITADO",
                    "Gestión de Laboratorios"
            );
        }

        throw new RuntimeException("Código UTP o usuario no encontrado en el sistema.");
    }

    // MÉTODO PARA RESTABLECER CONTRASEÑA (Desde el flujo de "Olvidé mi contraseña")
    @Transactional
    public void restablecerPasswordAlumno(String idAlumno, String nuevaPassword, String confirmacionPassword) {
        Alumno alumno = alumnoRepositorio.findById(idAlumno)
                .orElseThrow(() -> new CambioPasswordException("Alumno no encontrado"));

        if (!nuevaPassword.equals(confirmacionPassword)) {
            throw new CambioPasswordException("La nueva contraseña y su confirmación no coinciden.");
        }

        if (nuevaPassword.length() < 6) { // Ajustado a 6 para que coincida con tu HTML
            throw new CambioPasswordException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        alumno.setPassword(passwordEncoder.encode(nuevaPassword));
        alumnoRepositorio.save(alumno);
    }

    // MÉTODO PARA CAMBIAR CONTRASEÑA DESDE LA CUENTA (Logueado)
    @Transactional
    public void cambiarPasswordAlumno(String idAlumno, CambioPasswordDTO dto) {
        Alumno alumno = alumnoRepositorio.findById(idAlumno)
                .orElseThrow(() -> new CambioPasswordException("Alumno no encontrado"));

        // Validación Híbrida para la actual
        String passBD = alumno.getPassword();
        boolean actualValida = dto.getPasswordActual().equals(passBD);
        if (!actualValida) {
            try {
                actualValida = passwordEncoder.matches(dto.getPasswordActual(), passBD);
            } catch (Exception e) {}
        }

        if (!actualValida) {
            throw new CambioPasswordException("La contraseña actual es incorrecta.");
        }

        if (!dto.getPasswordNueva().equals(dto.getPasswordConfirmacion())) {
            throw new CambioPasswordException("La nueva contraseña y su confirmación no coinciden.");
        }

        // Validación Híbrida para la nueva (que no sea igual)
        boolean nuevaEsIgual = dto.getPasswordNueva().equals(passBD);
        if (!nuevaEsIgual) {
            try {
                nuevaEsIgual = passwordEncoder.matches(dto.getPasswordNueva(), passBD);
            } catch (Exception e) {}
        }

        if (nuevaEsIgual) {
            throw new CambioPasswordException("La nueva contraseña no puede ser igual a la anterior.");
        }

        // Guardamos en texto plano (para tu prueba de mañana)
        alumno.setPassword(dto.getPasswordNueva());
        alumnoRepositorio.save(alumno);
    }

    public static class ResultadoLogin {
        private final String rol;
        private final String codigo;
        private final String nombre;
        private final String apellido;
        private final String estadoCuenta;
        private final String carrera;

        public ResultadoLogin(String rol, String codigo, String nombre, String apellido, String estadoCuenta, String carrera) {
            this.rol = rol;
            this.codigo = codigo;
            this.nombre = nombre;
            this.apellido = apellido;
            this.estadoCuenta = estadoCuenta;
            this.carrera = carrera;
        }

        public String getRol() { return rol; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public String getEstadoCuenta() { return estadoCuenta; }
        public String getCarrera() { return carrera; }
        public String getNombreCompleto() { return nombre + " " + apellido; }
        public boolean esAdmin() { return "ADMIN".equalsIgnoreCase(rol); }
        public boolean esAlumno() { return "ALUMNO".equalsIgnoreCase(rol); }
    }
}
