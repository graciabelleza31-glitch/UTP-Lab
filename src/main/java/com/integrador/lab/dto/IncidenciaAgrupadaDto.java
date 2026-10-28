package com.integrador.lab.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class IncidenciaAgrupadaDto {
        private String idReserva;
        private String laboratorio;
        private String motivo;
        private BigDecimal montoTotal;
        private String estado;
        private List<String> alumnos;
        private List<String> correos;
        private LocalDateTime fechaNotificacion;
        
        
        public String getIdReserva() {
            return idReserva;
        }
        public void setIdReserva(String idReserva) {
            this.idReserva = idReserva;
        }
        public String getLaboratorio() {
            return laboratorio;
        }
        public void setLaboratorio(String laboratorio) {
            this.laboratorio = laboratorio;
        }
        public String getMotivo() {
            return motivo;
        }
        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
        public BigDecimal getMontoTotal() {
            return montoTotal;
        }
        public void setMontoTotal(BigDecimal montoTotal) {
            this.montoTotal = montoTotal;
        }
        public String getEstado() {
            return estado;
        }
        public void setEstado(String estado) {
            this.estado = estado;
        }
        public List<String> getAlumnos() {
            return alumnos;
        }
        public void setAlumnos(List<String> alumnos) {
            this.alumnos = alumnos;
        }
        public List<String> getCorreos() {
            return correos;
        }
        public void setCorreos(List<String> correos) {
            this.correos = correos;
        }
        public LocalDateTime getFechaNotificacion() {
            return fechaNotificacion;
        }
        public void setFechaNotificacion(LocalDateTime fechaNotificacion) {
            this.fechaNotificacion = fechaNotificacion;
        }

        
    }
