package com.soldout.inventario.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.inventario.entity.InventarioSeccion;
import com.soldout.inventario.entity.Seccion;
import com.soldout.inventario.repository.InventarioSeccionRepository;
import com.soldout.inventario.repository.SeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class InventarioConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioConsumer.class);

    private final SeccionRepository seccionRepository;
    private final InventarioSeccionRepository inventarioRepository;
    private final ObjectMapper objectMapper;

    public InventarioConsumer(SeccionRepository seccionRepository,
                              InventarioSeccionRepository inventarioRepository) {
        this.seccionRepository = seccionRepository;
        this.inventarioRepository = inventarioRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Consume evento.creado y inicializa el inventario de secciones.
     * Cuando un organizador publica un evento, este consumer crea
     * automáticamente las secciones y su inventario en db_inventario.
     */
    @KafkaListener(topics = "evento.creado", groupId = "grupo-inventario")
    @Transactional
    public void consumirEventoCreado(String mensaje) {
        log.info("KAFKA RECIBIDO - evento.creado en ms-inventario");
        try {
            JsonNode nodo = objectMapper.readTree(mensaje);
            UUID eventoId = UUID.fromString(nodo.get("evento_id").asText());
            JsonNode secciones = nodo.get("secciones");

            if (secciones == null || !secciones.isArray()) {
                log.warn("evento.creado sin secciones: {}", eventoId);
                return;
            }

            for (JsonNode seccionNode : secciones) {
                String nombre    = seccionNode.get("nombre").asText();
                String tipo      = seccionNode.get("tipo").asText();
                int capacidad    = seccionNode.get("capacidad").asInt();
                BigDecimal precio = new BigDecimal(seccionNode.get("precio").asText());

                // Verificar si ya existe la sección para evitar duplicados
                if (seccionRepository.existsByEventoIdAndNombre(eventoId, nombre)) {
                    log.info("Sección ya existe, omitiendo: {} - {}", eventoId, nombre);
                    continue;
                }

                // Crear la sección
                Seccion seccion = new Seccion();
                seccion.setEventoId(eventoId);
                seccion.setNombre(nombre);
                seccion.setTipo(tipo);
                seccion.setCapacidadTotal(capacidad);
                seccion.setPrecio(precio);
                Seccion seccionGuardada = seccionRepository.save(seccion);

                // Inicializar el inventario de la sección
                InventarioSeccion inventario = new InventarioSeccion();
                inventario.setSeccion(seccionGuardada);
                inventario.setTotalAsientos(capacidad);
                inventario.setAsientosDisponibles(capacidad);
                inventario.setAsientosReservados(0);
                inventario.setAsientosVendidos(0);
                inventarioRepository.save(inventario);

                log.info("INVENTARIO INICIALIZADO - evento: {}, seccion: {}, capacidad: {}",
                    eventoId, nombre, capacidad);
            }
        } catch (Exception e) {
            log.error("ERROR procesando evento.creado: {}", e.getMessage(), e);
        }
    }
}
