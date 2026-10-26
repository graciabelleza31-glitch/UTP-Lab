package com.integrador.lab.modelo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @Column(name = "id_reserva", length = 6)
    private String idReserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lab", nullable = false)
    private Laboratorio laboratorio;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(length = 20)
    private String estado;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL)
    private List<ReservaIntegrante> integrantes;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL)
    private List<ReservaItem> items;

    public Reserva() {
    }

    public Reserva(String idReserva, Laboratorio laboratorio, LocalDate fechaReserva, LocalTime horaInicio, LocalTime horaFin, String estado) {
        this.idReserva = idReserva;
        this.laboratorio = laboratorio;
        this.fechaReserva = fechaReserva;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = estado;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(String idReserva) {
        this.idReserva = idReserva;
    }

    public Laboratorio getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(Laboratorio laboratorio) {
        this.laboratorio = laboratorio;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDate fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<ReservaIntegrante> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(List<ReservaIntegrante> integrantes) {
        this.integrantes = integrantes;
    }

    public List<ReservaItem> getItems() {
        return items;
    }

    public void setItems(List<ReservaItem> items) {
        this.items = items;
    }
}

