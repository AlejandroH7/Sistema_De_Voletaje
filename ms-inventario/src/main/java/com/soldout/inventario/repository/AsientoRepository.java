package com.soldout.inventario.repository;

import com.soldout.inventario.entity.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsientoRepository extends JpaRepository<Asiento, UUID> {

    List<Asiento> findBySeccionId(UUID seccionId);

    List<Asiento> findBySeccionIdAndEstado(UUID seccionId, String estado);

    List<Asiento> findByMesaId(UUID mesaId);

    Optional<Asiento> findBySeccionIdAndNumeroAsiento(UUID seccionId, String numeroAsiento);

    int countBySeccionIdAndEstado(UUID seccionId, String estado);
}
