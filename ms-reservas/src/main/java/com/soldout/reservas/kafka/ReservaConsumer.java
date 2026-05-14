package com.soldout.reservas.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.reservas.service.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ReservaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReservaConsumer.class);
    private final ReservaService reservaService;
    private final ObjectMapper objectMapper;

    public ReservaConsumer(ReservaService reservaService) {
        this.reservaService = reservaService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "pago.confirmado", groupId = "grupo-reservas")
    public void consumirPagoConfirmado(String mensaje) {
        log.info("KAFKA RECIBIDO - pago.confirmado en ms-reservas");
        try {
            JsonNode nodo = objectMapper.readTree(mensaje);
            UUID reservaId = UUID.fromString(nodo.get("reserva_id").asText());
            reservaService.confirmarReserva(reservaId);
        } catch (Exception e) {
            log.error("ERROR procesando pago.confirmado: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "pago.fallido", groupId = "grupo-reservas")
    public void consumirPagoFallido(String mensaje) {
        log.info("KAFKA RECIBIDO - pago.fallido en ms-reservas");
        try {
            JsonNode nodo = objectMapper.readTree(mensaje);
            UUID reservaId = UUID.fromString(nodo.get("reserva_id").asText());
            reservaService.expirarReserva(reservaId);
        } catch (Exception e) {
            log.error("ERROR procesando pago.fallido: {}", e.getMessage(), e);
        }
    }
}
