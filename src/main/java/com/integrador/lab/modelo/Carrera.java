package com.integrador.lab.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "carrera")
public class Carrera {

    @Id
    @Column(name = "id_carrera", length = 5)
    private String idCarrera;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "permite_lab")
    private Boolean permiteLab;

    public Carrera() {
    }

    public Carrera(String idCarrera, String nombre, Boolean permiteLab) {
        this.idCarrera = idCarrera;
        this.nombre = nombre;
        this.permiteLab = permiteLab;
    }

    public String getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(String idCarrera) {
        this.idCarrera = idCarrera;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getPermiteLab() {
        return permiteLab;
    }

    public void setPermiteLab(Boolean permiteLab) {
        this.permiteLab = permiteLab;
    }
}

