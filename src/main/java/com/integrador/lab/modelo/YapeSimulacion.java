package com.integrador.lab.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "yape_simulacion")
public class YapeSimulacion {

    @Id
    @Column(name = "numero_celular", length = 9)
    private String numeroCelular;

    @Column(name = "nombre_titular", nullable = false, length = 100)
    private String nombreTitular;

    @Column(name = "codigo_aprobacion", nullable = false, length = 6)
    private String codigoAprobacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // getters y setters
    public String getNumeroCelular() { return numeroCelular; }
    public void setNumeroCelular(String numeroCelular) { this.numeroCelular = numeroCelular; }

    public String getNombreTitular() { return nombreTitular; }
    public void setNombreTitular(String nombreTitular) { this.nombreTitular = nombreTitular; }

    public String getCodigoAprobacion() { return codigoAprobacion; }
    public void setCodigoAprobacion(String codigoAprobacion) { this.codigoAprobacion = codigoAprobacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}