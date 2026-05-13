package com.soldout.inventario.repository;

import com.soldout.inventario.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeccionRepository extends JpaRepository<Seccion, UUID> {

    List<Seccion> findByEventoId(UUID eventoId);

    boolean existsByEventoIdAndNombre(UUID eventoId, String nombre);
}
