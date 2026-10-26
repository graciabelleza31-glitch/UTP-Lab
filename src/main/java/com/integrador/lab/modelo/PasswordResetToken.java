package com.integrador.lab.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @Column(name = "id_token", length = 10)
    private String idToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estud", nullable = false)
    private Alumno alumno;

    @Column(name = "codigo_temporal", length = 6, nullable = false)
    private String codigoTemporal;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @Column(nullable = false)
    private Boolean usado=false;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String idToken, Alumno alumno, String codigoTemporal, LocalDateTime fechaVencimiento, Boolean usado) {
        this.idToken = idToken;
        this.alumno = alumno;
        this.codigoTemporal = codigoTemporal;
        this.fechaVencimiento = fechaVencimiento;
        this.usado = usado;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public Alumno getAlumno() {
        return alumno;
    }

    public void setAlumno(Alumno alumno) {
        this.alumno = alumno;
    }

    public String getCodigoTemporal() {
        return codigoTemporal;
    }

    public void setCodigoTemporal(String codigoTemporal) {
        this.codigoTemporal = codigoTemporal;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Boolean getUsado() {
        return usado;
    }

    public void setUsado(Boolean usado) {
        this.usado = usado;
    }
}

