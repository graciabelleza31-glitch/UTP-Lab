package com.integrador.lab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.Sancion;

@Repository
public interface SancionRepository extends JpaRepository<Sancion, String> {

    List<Sancion> findByAlumno_IdEstudAndEstado(String idEstud, String estado);

    boolean existsByAlumno_IdEstudAndEstado(String idEstud, String estado);

    List<Sancion> findByEstado(String estado);

    // Búsqueda por estado para el Dashboard y el listado de Incidencias
    List<Sancion> findByAlumno_IdEstud(String idEstud);

    long countByAlumno_IdEstudAndEstadoNotIn(String idEstud, List<String> estados);

    
}

