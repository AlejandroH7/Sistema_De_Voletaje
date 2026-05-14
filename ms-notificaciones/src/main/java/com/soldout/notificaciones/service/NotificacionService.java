package com.soldout.notificaciones.service;

import com.soldout.notificaciones.entity.Notificacion;
import com.soldout.notificaciones.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificacionService {

    private static final Logger log =
        LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Envía notificación con deduplicación por clave_idempotencia.
     * Si ya existe una notificación con esa clave la ignora.
     */
    @Transactional
    public void enviarNotificacion(UUID usuarioId, UUID reservaId,
                                    String tipo, String asunto,
                                    String contenido, String claveBase) {

        String claveIdempotencia = generarClave(claveBase);

        // DEDUPLICACIÓN: si ya existe, ignorar
        if (notificacionRepository.existsByClaveIdempotencia(claveIdempotencia)) {
            log.info("NOTIFICACION DUPLICADA - ignorada: {}", claveIdempotencia);
            return;
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setReservaId(reservaId);
        notificacion.setTipo(tipo);
        notificacion.setCanal("CORREO");
        notificacion.setAsunto(asunto);
        notificacion.setContenido(contenido);
        notificacion.setEstado("PENDIENTE");
        notificacion.setClaveIdempotencia(claveIdempotencia);

        notificacionRepository.save(notificacion);

        // Simular envío (en desarrollo: solo log)
        log.info("NOTIFICACION ENVIADA - tipo: {}, usuario: {}", tipo, usuarioId);

        notificacion.setEstado("ENVIADO");
        notificacion.setEnviadoEn(LocalDateTime.now());
        notificacionRepository.save(notificacion);
    }

    private String generarClave(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 64);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generando clave", e);
        }
    }
}
