package com.integrador.lab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.Boleta;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, String> {

    List<Boleta> findByAlumno_IdEstudOrderByFechaEmisionDesc(String idEstud);
}
