package com.soldout.reservas.repository;

import com.soldout.reservas.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    // CRÍTICO: verifica idempotencia antes de crear reserva
    boolean existsByClaveIdempotencia(String claveIdempotencia);

    // Retorna reserva existente en caso de reintento
    Optional<Reserva> findByClaveIdempotencia(String claveIdempotencia);

    // Historial de reservas por usuario
    List<Reserva> findByUsuarioId(UUID usuarioId);

    // Reservas por evento
    List<Reserva> findByEventoId(UUID eventoId);

    // Reservas por estado
    List<Reserva> findByEstado(String estado);

    // CRÍTICO: usado por el scheduler para detectar reservas expiradas
    // Busca reservas PENDIENTE cuyo tiempo de expiración ya pasó
    List<Reserva> findByEstadoAndExpiraEnBefore(String estado, LocalDateTime fecha);
}
