package com.soldout.eventos.repository;

import com.soldout.eventos.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    // Listar eventos por estado (ej: ACTIVO)
    List<Evento> findByEstado(String estado);

    // Eventos de un venue específico
    List<Evento> findByLugarId(UUID lugarId);

    // Verificar si ya existe un evento con ese nombre y fecha
    // Evita crear eventos duplicados
    boolean existsByNombreAndFechaEvento(String nombre, LocalDateTime fechaEvento);
}
