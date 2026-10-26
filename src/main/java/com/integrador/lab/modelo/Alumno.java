package com.integrador.lab.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alumno")
public class Alumno {

    @Id
    @Column(name = "id_estud", length = 9)
    private String idEstud;

    @Column(length = 15, unique = true)
    private String dni;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "es_matriculado")
    private Boolean esMatriculado;

    @Column(name = "estado_cuenta", length = 20)
    private String estadoCuenta;

    public Alumno() {
    }

    public Alumno(String idEstud, String dni, String password, String nombre, String apellido, Carrera carrera, Boolean esMatriculado, String estadoCuenta) {
        this.idEstud = idEstud;
        this.dni = dni;
        this.password = password;
        this.nombre = nombre;
        this.apellido = apellido;
        this.carrera = carrera;
        this.esMatriculado = esMatriculado;
        this.estadoCuenta = estadoCuenta;
    }

    // Getters y Setters explícitos
    public String getIdEstud() {
        return idEstud;
    }

    public void setIdEstud(String idEstud) {
        this.idEstud = idEstud;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
    }

    public Boolean getEsMatriculado() {
        return esMatriculado;
    }

    public void setEsMatriculado(Boolean esMatriculado) {
        this.esMatriculado = esMatriculado;
    }

    public String getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(String estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }
}
