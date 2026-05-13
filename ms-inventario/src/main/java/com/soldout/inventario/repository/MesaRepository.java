package com.soldout.inventario.repository;

import com.soldout.inventario.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MesaRepository extends JpaRepository<Mesa, UUID> {

    List<Mesa> findBySeccionId(UUID seccionId);

    List<Mesa> findBySeccionIdAndEstado(UUID seccionId, String estado);
}
