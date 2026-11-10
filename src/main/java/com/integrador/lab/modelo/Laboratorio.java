package com.integrador.lab.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_encargado")
    private Encargado encargado;

    public Laboratorio() {}

    public String getIdLab() { return idLab; }
    public void setIdLab(String idLab) { this.idLab = idLab; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getUbicacionCubiculo() { return ubicacionCubiculo; }
    public void setUbicacionCubiculo(String ubicacionCubiculo) { this.ubicacionCubiculo = ubicacionCubiculo; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Encargado getEncargado() { return encargado; }
    public void setEncargado(Encargado encargado) { this.encargado = encargado; }
}