package com.integrador.lab.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.integrador.lab.modelo.YapeSimulacion;

@Repository
public interface YapeSimulacionRepository extends JpaRepository<YapeSimulacion, String> {
    Optional<YapeSimulacion> findByNumeroCelular(String numeroCelular);
}