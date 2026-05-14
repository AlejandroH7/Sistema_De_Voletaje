package com.soldout.notificaciones.controller;

import com.soldout.notificaciones.dto.RespuestaApi;
import com.soldout.notificaciones.entity.Notificacion;
import com.soldout.notificaciones.repository.NotificacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<RespuestaApi<List<Notificacion>>> obtenerPorUsuario(
            @PathVariable UUID usuarioId) {
        List<Notificacion> notificaciones =
            notificacionRepository.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(
            RespuestaApi.exito("Notificaciones obtenidas", notificaciones));
    }
}
