package com.soldout.inventario.repository;

import com.soldout.inventario.entity.InventarioSeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventarioSeccionRepository extends JpaRepository<InventarioSeccion, UUID> {

    Optional<InventarioSeccion> findBySeccionId(UUID seccionId);

    @Modifying
    @Query("""
            UPDATE InventarioSeccion i
            SET i.asientosDisponibles = i.asientosDisponibles - :cantidad,
                i.asientosReservados = i.asientosReservados + :cantidad
            WHERE i.seccion.id = :seccionId
              AND i.asientosDisponibles >= :cantidad
            """)
    int reservarAsientos(@Param("seccionId") UUID seccionId, @Param("cantidad") int cantidad);

    @Modifying
    @Query("""
            UPDATE InventarioSeccion i
            SET i.asientosDisponibles = i.asientosDisponibles + :cantidad,
                i.asientosReservados = i.asientosReservados - :cantidad
            WHERE i.seccion.id = :seccionId
            """)
    int liberarAsientos(@Param("seccionId") UUID seccionId, @Param("cantidad") int cantidad);
}
