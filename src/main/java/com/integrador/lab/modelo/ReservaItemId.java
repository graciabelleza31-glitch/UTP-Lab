package com.integrador.lab.modelo;

import java.io.Serializable;
import java.util.Objects;

public class ReservaItemId implements Serializable {

    private String reserva;
    private String producto;

    public ReservaItemId() {
    }

    public ReservaItemId(String reserva, String producto) {
        this.reserva = reserva;
        this.producto = producto;
    }

    public String getReserva() {
        return reserva;
    }

    public void setReserva(String reserva) {
        this.reserva = reserva;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReservaItemId that = (ReservaItemId) o;
        return Objects.equals(reserva, that.reserva) && Objects.equals(producto, that.producto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reserva, producto);
    }
}

