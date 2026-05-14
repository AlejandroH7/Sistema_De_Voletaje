package com.soldout.notificaciones.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class NotificacionConsumer {

    private static final Logger log =
        LoggerFactory.getLogger(NotificacionConsumer.class);

    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    public NotificacionConsumer(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "pago.confirmado", groupId = "grupo-notificaciones")
    public void consumirPagoConfirmado(String mensaje) {
        log.info("KAFKA RECIBIDO - pago.confirmado en ms-notificaciones");
        try {
            JsonNode nodo = objectMapper.readTree(mensaje);
            UUID usuarioId = UUID.fromString(nodo.get("usuario_id").asText());
            UUID reservaId = UUID.fromString(nodo.get("reserva_id").asText());
            String pagoId  = nodo.get("pago_id").asText();

            notificacionService.enviarNotificacion(
                usuarioId, reservaId,
                "RESERVA_CONFIRMADA",
                "Tu compra fue confirmada",
                "Tu reserva ha sido confirmada. Disfruta el evento.",
                pagoId + ":RESERVA_CONFIRMADA:" + usuarioId
            );
        } catch (Exception e) {
            log.error("ERROR procesando pago.confirmado: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "pago.fallido", groupId = "grupo-notificaciones")
    public void consumirPagoFallido(String mensaje) {
        log.info("KAFKA RECIBIDO - pago.fallido en ms-notificaciones");
        try {
            JsonNode nodo = objectMapper.readTree(mensaje);
            UUID usuarioId = UUID.fromString(nodo.get("usuario_id").asText());
            UUID reservaId = UUID.fromString(nodo.get("reserva_id").asText());
            String pagoId  = nodo.get("pago_id").asText();

            notificacionService.enviarNotificacion(
                usuarioId, reservaId,
                "PAGO_FALLIDO",
                "Tu pago no pudo procesarse",
                "Lo sentimos, tu pago fue rechazado. Por favor intenta de nuevo.",
                pagoId + ":PAGO_FALLIDO:" + usuarioId
            );
        } catch (Exception e) {
            log.error("ERROR procesando pago.fallido: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "reserva.expirada", groupId = "grupo-notificaciones")
    public void consumirReservaExpirada(String mensaje) {
        log.info("KAFKA RECIBIDO - reserva.expirada en ms-notificaciones");
        try {
            JsonNode nodo = objectMapper.readTree(mensaje);
            UUID usuarioId = UUID.fromString(nodo.get("usuario_id").asText());
            UUID reservaId = UUID.fromString(nodo.get("reserva_id").asText());

            notificacionService.enviarNotificacion(
                usuarioId, reservaId,
                "RESERVA_EXPIRADA",
                "Tu reserva ha expirado",
                "Tu reserva expiró porque no se completó el pago en 10 minutos.",
                reservaId + ":RESERVA_EXPIRADA:" + usuarioId
            );
        } catch (Exception e) {
            log.error("ERROR procesando reserva.expirada: {}", e.getMessage(), e);
        }
    }
}
