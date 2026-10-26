package com.integrador.lab.modelo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "laboratorio")
public class Laboratorio {

    @Id
    @Column(name = "id_lab", length = 6)
    private String idLab;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "ubicacion_cubiculo", length = 50)
    private String ubicacionCubiculo;

    public Laboratorio() {
    }

    public Laboratorio(String idLab, String nombre, String tipo, String ubicacionCubiculo) {
        this.idLab = idLab;
        this.nombre = nombre;
        this.tipo = tipo;
        this.ubicacionCubiculo = ubicacionCubiculo;
    }

    public String getIdLab() {
        return idLab;
    }

    public void setIdLab(String idLab) {
        this.idLab = idLab;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getUbicacionCubiculo() {
        return ubicacionCubiculo;
    }

    public void setUbicacionCubiculo(String ubicacionCubiculo) {
        this.ubicacionCubiculo = ubicacionCubiculo;
    }
}

