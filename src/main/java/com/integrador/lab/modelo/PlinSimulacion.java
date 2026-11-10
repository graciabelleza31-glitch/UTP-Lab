package com.integrador.lab.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plin_simulacion")
public class PlinSimulacion {

    @Id
    @Column(name = "numero_celular", length = 9)
    private String numeroCelular;

    @Column(name = "nombre_titular", nullable = false, length = 100)
    private String nombreTitular;

    @Column(name = "codigo_operacion", nullable = false, length = 8)
    private String codigoOperacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    public String getNumeroCelular() { return numeroCelular; }
    public void setNumeroCelular(String numeroCelular) { this.numeroCelular = numeroCelular; }

    public String getNombreTitular() { return nombreTitular; }
    public void setNombreTitular(String nombreTitular) { this.nombreTitular = nombreTitular; }

    public String getCodigoOperacion() { return codigoOperacion; }
    public void setCodigoOperacion(String codigoOperacion) { this.codigoOperacion = codigoOperacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}