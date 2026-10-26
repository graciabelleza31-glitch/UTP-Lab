package com.integrador.lab.modelo;

import java.io.Serializable;
import java.util.Objects;

public class ReservaIntegranteId implements Serializable {

    private String reserva;
    private String alumno;

    public ReservaIntegranteId() {
    }

    public ReservaIntegranteId(String reserva, String alumno) {
        this.reserva = reserva;
        this.alumno = alumno;
    }

    public String getReserva() {
        return reserva;
    }

    public void setReserva(String reserva) {
        this.reserva = reserva;
    }

    public String getAlumno() {
        return alumno;
    }

    public void setAlumno(String alumno) {
        this.alumno = alumno;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReservaIntegranteId that = (ReservaIntegranteId) o;
        return Objects.equals(reserva, that.reserva) && Objects.equals(alumno, that.alumno);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reserva, alumno);
    }
}

