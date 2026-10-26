package com.integrador.lab.modelo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @Column(name = "id_producto", length = 6)
    private String idProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lab", nullable = true)
    private Laboratorio laboratorio;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(name = "es_venta")
    private Boolean esVenta;

    @Column(name = "precio_venta", precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "costo_reposicion", precision = 10, scale = 2)
    private BigDecimal costoReposicion;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    public Producto() {
    }

    public Producto(String idProducto, Laboratorio laboratorio, String nombre, String categoria, Boolean esVenta, BigDecimal precioVenta, BigDecimal costoReposicion, Integer stockActual, Integer stockMinimo) {
        this.idProducto = idProducto;
        this.laboratorio = laboratorio;
        this.nombre = nombre;
        this.categoria = categoria;
        this.esVenta = esVenta;
        this.precioVenta = precioVenta;
        this.costoReposicion = costoReposicion;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public Laboratorio getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(Laboratorio laboratorio) {
        this.laboratorio = laboratorio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Boolean getEsVenta() {
        return esVenta;
    }

    public void setEsVenta(Boolean esVenta) {
        this.esVenta = esVenta;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getCostoReposicion() {
        return costoReposicion;
    }

    public void setCostoReposicion(BigDecimal costoReposicion) {
        this.costoReposicion = costoReposicion;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}

