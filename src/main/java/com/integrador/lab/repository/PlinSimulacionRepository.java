package com.integrador.lab.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.integrador.lab.modelo.PlinSimulacion;

@Repository
public interface PlinSimulacionRepository extends JpaRepository<PlinSimulacion, String> {
    Optional<PlinSimulacion> findByNumeroCelular(String numeroCelular);
}