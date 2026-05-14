package com.soldout.usuarios.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.usuarios.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class UsuarioProducer {

    private static final Logger log = LoggerFactory.getLogger(UsuarioProducer.class);
    private static final String TOPIC = "usuario.registrado";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UsuarioProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publicarUsuarioRegistrado(Usuario usuario) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("usuario_id", usuario.getId().toString());
            payload.put("nombre",     usuario.getNombre());
            payload.put("email",      usuario.getEmail());
            payload.put("rol",        usuario.getRol());

            String mensaje = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC, usuario.getId().toString(), mensaje);
            log.info("KAFKA PUBLICADO - usuario.registrado: {}", usuario.getId());
        } catch (Exception e) {
            log.error("ERROR publicando usuario.registrado: {}", e.getMessage(), e);
        }
    }
}
