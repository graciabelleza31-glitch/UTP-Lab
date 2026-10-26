package com.integrador.lab.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {

    List<Producto> findByLaboratorio_IdLab(String idLab);

    List<Producto> findByLaboratorio_IdLabAndCategoria(String idLab, String categoria);

    List<Producto> findByLaboratorioIsNull();

    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo")
    List<Producto> findProductosStockCritico();

    @Query("SELECT p FROM Producto p WHERE (p.laboratorio.idLab = :idLab OR p.laboratorio IS NULL) AND p.stockActual > 0")
    List<Producto> findDisponiblesPorLab(@Param("idLab") String idLab);
}
