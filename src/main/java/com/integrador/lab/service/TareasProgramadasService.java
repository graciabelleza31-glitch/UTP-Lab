package com.integrador.lab.service;

import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TareasProgramadasService {

    @Autowired
    private ReservaRepository reservaRepository;

    // 🔑 Zona horaria de Perú (UTC-5)
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    // Tiempo de tolerancia en minutos después de la hora de inicio
    private static final int MINUTOS_TOLERANCIA = 10;

    // =========================================================================
    // TAREA 1: CANCELAR RESERVAS NO ASISTIDAS (Las que se quedaron en CONFIRMADA)
    // =========================================================================
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void cancelarReservasNoAsistidas() {
        LocalDate hoy = LocalDate.now(ZONA_PERU);
        LocalTime ahora = LocalTime.now(ZONA_PERU);
        LocalDateTime ahoraCompleto = LocalDateTime.now(ZONA_PERU);

        List<Reserva> reservasConfirmadas = reservaRepository.findByEstado("CONFIRMADA");

        System.out.println("⏰ [JOB] " + ahoraCompleto + 
                " (Perú: " + ahora + ")" +
                " - Reservas CONFIRMADAS encontradas: " + reservasConfirmadas.size());

        int canceladas = 0;
        for (Reserva r : reservasConfirmadas) {
            if (r.getHoraInicio() == null) continue;

            boolean noAsistio = false;

            // Caso 1: Es de HOY y pasaron más de 10 minutos desde hora_inicio
            if (r.getFechaReserva().equals(hoy)) {
                LocalTime limiteTolerancia = r.getHoraInicio().plusMinutes(MINUTOS_TOLERANCIA);
                if (ahora.isAfter(limiteTolerancia)) {
                    noAsistio = true;
                }
            }
            // Caso 2: Es de un día ANTERIOR (nunca hizo check-in)
            else if (r.getFechaReserva().isBefore(hoy)) {
                noAsistio = true;
            }

            if (noAsistio) {
                r.setEstado("NO_ASISTIO");
                reservaRepository.save(r);
                canceladas++;
                System.out.println("   ❌ " + r.getIdReserva() + 
                        " cancelada por NO ASISTIR (fecha: " + r.getFechaReserva() + 
                        ", hora_inicio: " + r.getHoraInicio() + ")");
            }
        }

        if (canceladas > 0) {
            System.out.println("[" + LocalDateTime.now(ZONA_PERU) + "] Total canceladas: " + canceladas);
        }
    }

    // =========================================================================
    // TAREA 2: EXPIRAR RESERVAS EN USO (Si pasó la hora y no hicieron checkout)
    // =========================================================================
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirarReservasAntiguas() {
        LocalDate hoy = LocalDate.now();
        LocalTime horaActual = LocalTime.now();
        
        List<Reserva> candidatas = reservaRepository.findByEstadoAndFechaReservaLessThanEqual("EN_USO", hoy);

        int contador = 0;
        for (Reserva reserva : candidatas) {
            boolean esDeDiasPasados = reserva.getFechaReserva().isBefore(hoy);
            boolean esDeHoyPeroYaTermino = reserva.getFechaReserva().isEqual(hoy) && reserva.getHoraFin().isBefore(horaActual);

            if (esDeDiasPasados || esDeHoyPeroYaTermino) {
                reserva.setEstado("DEVUELTA");
                reservaRepository.save(reserva);
                contador++;
            }
        }

        if (contador > 0) {
            System.out.println("⏰ [SCHEDULER] Se cerraron " + contador + " reservas.");
        }
    }

    // =========================================================================
    // TAREA 3: VENCER INSPECCIONES (Si el admin no revisó en 30 min)
    // =========================================================================
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void vencerInspecciones() {
        LocalDate hoy = LocalDate.now();

        List<Reserva> reservasPendientes = reservaRepository.findByEstadoAndFechaReservaLessThanEqual("DEVUELTA", hoy);
        
        for (Reserva r : reservasPendientes) {
            LocalDateTime horaFin = LocalDateTime.of(r.getFechaReserva(), r.getHoraFin());
            LocalDateTime limite = horaFin.plusMinutes(30);

            if (LocalDateTime.now().isAfter(limite)) {
                r.setEstado("NO_INSPECCIONADO");
                reservaRepository.save(r);
                System.out.println("⚠️ [SCHEDULER] Reserva " + r.getIdReserva() + " pasó a NO_INSPECCIONADO.");
            }
        }
    }
}
