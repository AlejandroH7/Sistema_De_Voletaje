package com.soldout.reservas.repository;

import com.soldout.reservas.entity.DetalleReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, UUID> {

    // Obtener todos los detalles de una reserva
    List<DetalleReserva> findByReservaId(UUID reservaId);

    // Obtener detalles por sección
    List<DetalleReserva> findBySeccionId(UUID seccionId);
}
