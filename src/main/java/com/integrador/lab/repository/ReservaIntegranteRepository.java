package com.integrador.lab.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.ReservaIntegrante;
import com.integrador.lab.modelo.ReservaIntegranteId;

@Repository
public interface ReservaIntegranteRepository extends JpaRepository<ReservaIntegrante, ReservaIntegranteId> {

    List<ReservaIntegrante> findByReserva_IdReserva(String idReserva);

    List<ReservaIntegrante> findByAlumno_IdEstud(String idEstud);
}
