package com.integrador.lab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.DetalleBoleta;

@Repository
public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, String> {

    List<DetalleBoleta> findByBoleta_IdBoleta(String idBoleta);
}
