package com.integrador.lab.repository;

import com.integrador.lab.modelo.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface CarreraRepository extends JpaRepository<Carrera, String> {
}
