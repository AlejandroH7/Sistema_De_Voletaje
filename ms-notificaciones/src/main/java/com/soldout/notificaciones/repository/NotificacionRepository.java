package com.soldout.notificaciones.repository;

import com.soldout.notificaciones.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    Optional<Notificacion> findByClaveIdempotencia(String claveIdempotencia);

    List<Notificacion> findByUsuarioId(UUID usuarioId);

    List<Notificacion> findByUsuarioIdAndTipo(UUID usuarioId, String tipo);
}
