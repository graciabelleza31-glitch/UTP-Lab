package com.integrador.lab.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.Laboratorio;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, String> {

    List<Laboratorio> findByTipoIgnoreCase(String tipo);

    List<Laboratorio> findByUbicacionCubiculoStartingWith(String bloque);

    // NUEVO: Consulta filtrando por los permisos de la carrera
    @Query(value = """
        SELECT l.* 
        FROM laboratorio l
        INNER JOIN carrera_permiso_lab cpl ON LOWER(l.tipo) = LOWER(cpl.tipo_lab)
        WHERE cpl.id_carrera = :idCarrera
        ORDER BY l.nombre ASC, l.ubicacion_cubiculo ASC
    """, nativeQuery = true)
    List<Laboratorio> findLaboratoriosPermitidosPorCarrera(@Param("idCarrera") String idCarrera);
}
