package com.soldout.pagos.service;

import com.soldout.pagos.entity.Pago;
import com.soldout.pagos.exception.NegocioException;
import com.soldout.pagos.kafka.PagoProducer;
import com.soldout.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final PagoProducer pagoProducer;

    public PagoService(PagoRepository pagoRepository, PagoProducer pagoProducer) {
        this.pagoRepository = pagoRepository;
        this.pagoProducer = pagoProducer;
    }

    @Transactional(readOnly = true)
    public Optional<Pago> verificarIdempotencia(String claveIdempotencia) {
        return pagoRepository.findByClaveIdempotencia(claveIdempotencia);
    }

    @Transactional
    public Pago procesarPago(UUID reservaId, UUID usuarioId,
                              BigDecimal monto, String metodoPago,
                              String claveIdempotencia) {

        log.info("PAGO - Procesando: reserva={}, monto={}", reservaId, monto);

        // PASO 1: Idempotencia
        Optional<Pago> existente = verificarIdempotencia(claveIdempotencia);
        if (existente.isPresent()) {
            log.info("PAGO DUPLICADO - retornando existente: {}", claveIdempotencia);
            return existente.get();
        }

        // PASO 2: Verificar que no haya pago previo para esta reserva
        pagoRepository.findByReservaId(reservaId).ifPresent(p -> {
            throw new NegocioException(
                "Ya existe un pago para esta reserva",
                "PAGO_YA_PROCESADO",
                HttpStatus.CONFLICT
            );
        });

        // PASO 3: Crear registro PENDIENTE
        Pago pago = new Pago();
        pago.setReservaId(reservaId);
        pago.setUsuarioId(usuarioId);
        pago.setMonto(monto);
        pago.setMoneda("GTQ");
        pago.setEstado("PENDIENTE");
        pago.setClaveIdempotencia(claveIdempotencia);
        pago.setMetodoPago(metodoPago != null ? metodoPago : "TARJETA");

        Pago guardado = pagoRepository.save(pago);
        log.info("PAGO PENDIENTE creado: {}", guardado.getId());

        // PASO 4: Simular procesamiento 70% éxito 30% fallo
        boolean exitoso = Math.random() < 0.70;
        log.info("PAGO SIMULADO - resultado: {}", exitoso ? "EXITOSO" : "FALLIDO");

        // PASO 5: Actualizar estado
        guardado.setEstado(exitoso ? "COMPLETADO" : "FALLIDO");
        guardado.setProcesadoEn(LocalDateTime.now());
        if (!exitoso) {
            guardado.setMotivoFallo("Pago rechazado por el procesador");
        }
        pagoRepository.save(guardado);

        // PASO 6: Guardar en Outbox
        pagoProducer.guardarEnOutbox(guardado);

        log.info("PAGO PROCESADO - id: {}, estado: {}",
            guardado.getId(), guardado.getEstado());
        return guardado;
    }

    @Transactional(readOnly = true)
    public Pago obtenerPorId(UUID id) {
        return pagoRepository.findById(id)
            .orElseThrow(() -> new NegocioException(
                "Pago no encontrado", "PAGO_NO_ENCONTRADO", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Pago obtenerPorReservaId(UUID reservaId) {
        return pagoRepository.findByReservaId(reservaId)
            .orElseThrow(() -> new NegocioException(
                "No hay pago para esa reserva",
                "PAGO_NO_ENCONTRADO", HttpStatus.NOT_FOUND));
    }
}
