package com.integrador.lab.controller.alumno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.lab.modelo.Reserva;
import com.integrador.lab.repository.ReservaRepository;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioRestController {

    @Autowired
    private ReservaRepository reservaRepositorio;

    @GetMapping("/{idLab}/horarios-ocupados")
    public ResponseEntity<List<String>> obtenerHorariosOcupados(
            @PathVariable("idLab") String idLab,
            @RequestParam("fecha") String fechaStr) {

        LocalDate fecha = LocalDate.parse(fechaStr);
        List<Reserva> reservas = reservaRepositorio.findByLaboratorio_IdLabAndFechaReserva(idLab, fecha);
        List<String> horasOcupadas = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Reserva r : reservas) {
            if (!"CANCELADA".equalsIgnoreCase(r.getEstado())) {
                String inicio = r.getHoraInicio().format(fmt);
                horasOcupadas.add(inicio);
                // Si la reserva dura 2 horas, bloqueamos también la hora intermedia
                if (r.getHoraFin().isAfter(r.getHoraInicio().plusHours(1))) {
                    horasOcupadas.add(r.getHoraInicio().plusHours(1).format(fmt));
                }
            }
        }

        return ResponseEntity.ok(horasOcupadas);
    }
}

