package com.soldout.reservas.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.reservas.entity.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ReservaProducer {

    private static final Logger log = LoggerFactory.getLogger(ReservaProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ReservaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publicarReservaCreada(Reserva reserva) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reserva_id", reserva.getId().toString());
            payload.put("usuario_id", reserva.getUsuarioId().toString());
            payload.put("evento_id",  reserva.getEventoId().toString());
            payload.put("monto",      reserva.getPrecioTotal());
            payload.put("expira_en",  reserva.getExpiraEn().toString());

            String mensaje = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("reserva.creada", reserva.getId().toString(), mensaje);
            log.info("KAFKA PUBLICADO - reserva.creada: {}", reserva.getId());
        } catch (Exception e) {
            log.error("ERROR publicando reserva.creada: {}", e.getMessage(), e);
        }
    }

    public void publicarReservaExpirada(Reserva reserva) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reserva_id", reserva.getId().toString());
            payload.put("usuario_id", reserva.getUsuarioId().toString());
            payload.put("evento_id",  reserva.getEventoId().toString());

            String mensaje = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("reserva.expirada", reserva.getId().toString(), mensaje);
            log.info("KAFKA PUBLICADO - reserva.expirada: {}", reserva.getId());
        } catch (Exception e) {
            log.error("ERROR publicando reserva.expirada: {}", e.getMessage(), e);
        }
    }

    public void publicarReservaConfirmada(Reserva reserva) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reserva_id", reserva.getId().toString());
            payload.put("usuario_id", reserva.getUsuarioId().toString());

            String mensaje = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("reserva.confirmada", reserva.getId().toString(), mensaje);
            log.info("KAFKA PUBLICADO - reserva.confirmada: {}", reserva.getId());
        } catch (Exception e) {
            log.error("ERROR publicando reserva.confirmada: {}", e.getMessage(), e);
        }
    }
}
