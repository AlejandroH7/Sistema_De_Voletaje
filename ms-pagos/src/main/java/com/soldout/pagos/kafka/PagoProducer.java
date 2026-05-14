package com.soldout.pagos.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soldout.pagos.entity.Pago;
import com.soldout.pagos.entity.PagosSalida;
import com.soldout.pagos.repository.PagosSalidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PagoProducer {

    private static final Logger log = LoggerFactory.getLogger(PagoProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PagosSalidaRepository salidaRepository;
    private final ObjectMapper objectMapper;

    public PagoProducer(KafkaTemplate<String, String> kafkaTemplate,
                        PagosSalidaRepository salidaRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.salidaRepository = salidaRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void guardarEnOutbox(Pago pago) {
        try {
            String tipoEvento = "COMPLETADO".equals(pago.getEstado())
                ? "pago.confirmado" : "pago.fallido";

            Map<String, Object> payload = new HashMap<>();
            payload.put("pago_id",    pago.getId().toString());
            payload.put("reserva_id", pago.getReservaId().toString());
            payload.put("usuario_id", pago.getUsuarioId().toString());
            payload.put("monto",      pago.getMonto());
            payload.put("estado",     pago.getEstado());

            PagosSalida salida = new PagosSalida();
            salida.setIdAgregado(pago.getId());
            salida.setTipoEvento(tipoEvento);
            salida.setPayload(objectMapper.writeValueAsString(payload));
            salida.setPublicado(false);

            salidaRepository.save(salida);
            log.info("OUTBOX GUARDADO - tipo: {}, pago: {}", tipoEvento, pago.getId());
        } catch (Exception e) {
            log.error("ERROR guardando en Outbox: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publicarEventosPendientes() {
        List<PagosSalida> pendientes =
            salidaRepository.findByPublicadoFalseOrderByCreadoEnAsc();

        if (pendientes.isEmpty()) return;

        log.info("OUTBOX SCHEDULER - {} eventos pendientes", pendientes.size());

        for (PagosSalida evento : pendientes) {
            try {
                kafkaTemplate.send(
                    evento.getTipoEvento(),
                    evento.getIdAgregado().toString(),
                    evento.getPayload()
                ).get();

                evento.setPublicado(true);
                evento.setPublicadoEn(LocalDateTime.now());
                salidaRepository.save(evento);

                log.info("OUTBOX PUBLICADO - topic: {}, id: {}",
                    evento.getTipoEvento(), evento.getIdAgregado());
            } catch (Exception e) {
                log.error("ERROR publicando a Kafka, reintentará: {}", e.getMessage());
            }
        }
    }
}
