package com.integrador.lab.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.TarjetaSimulacion;

@Repository
public interface TarjetaSimulacionRepository extends JpaRepository<TarjetaSimulacion, String> {
    Optional<TarjetaSimulacion> findByNumeroTarjeta(String numeroTarjeta);
}