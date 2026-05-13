package com.soldout.eventos.repository;

import com.soldout.eventos.entity.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LugarRepository extends JpaRepository<Lugar, UUID> {

    // Buscar venues por ciudad
    List<Lugar> findByCiudad(String ciudad);

    // Verificar si ya existe un venue con ese nombre
    boolean existsByNombre(String nombre);
}
