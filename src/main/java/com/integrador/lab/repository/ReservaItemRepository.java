package com.integrador.lab.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.integrador.lab.modelo.ReservaItem;
import com.integrador.lab.modelo.ReservaItemId;

@Repository
public interface ReservaItemRepository extends JpaRepository<ReservaItem, ReservaItemId> {

    List<ReservaItem> findByReserva_IdReserva(String idReserva);
}
