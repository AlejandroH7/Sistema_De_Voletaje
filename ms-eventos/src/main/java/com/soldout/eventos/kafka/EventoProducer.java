package com.soldout.eventos.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.eventos.dto.CrearEventoRequest;
import com.soldout.eventos.entity.Evento;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventoProducer {

    private static final Logger log = LoggerFactory.getLogger(EventoProducer.class);
    private static final String TOPIC = "evento.creado";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventoProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publicarEventoCreado(
            Evento evento,
            List<CrearEventoRequest.SeccionRequest> secciones) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento_id", evento.getId().toString());
            payload.put("tipo_evento", evento.getTipoEvento());
            payload.put("secciones", secciones != null ? secciones.stream().map(s -> {
                Map<String, Object> sec = new HashMap<>();
                sec.put("nombre", s.getNombre());
                sec.put("tipo", s.getTipo());
                sec.put("capacidad", s.getCapacidad());
                sec.put("precio", s.getPrecio());
                return sec;
            }).toList() : List.of());

            String mensaje = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC, evento.getId().toString(), mensaje);
            log.info("KAFKA PUBLICADO - evento.creado: eventoId: {}", evento.getId());
        } catch (Exception e) {
            log.error("ERROR publicando evento.creado: {}", e.getMessage(), e);
        }
    }
}
