package com.soldout.reservas.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Orquestador de la Saga de compra de boletos.
 *
 * FLUJO DE LA SAGA:
 *
 * PASO 1 — iniciarSaga():
 *   → Verificar que el evento existe y está ACTIVO
 *   → Llamar a ms-inventario para reservar asientos
 *   → Crear reserva en BD con estado PENDIENTE
 *   → Guardar TTL en Redis: SET reserva:ttl:{id} "activa" EX 600
 *   → Publicar reserva.creada a Kafka
 *
 * PASO 2 — confirmarSaga():
 *   → Se activa cuando ms-pagos publica pago.confirmado a Kafka
 *   → Cambiar estado de reserva de PENDIENTE a CONFIRMADO
 *   → Eliminar TTL de Redis
 *   → Publicar reserva.confirmada a Kafka
 *
 * PASO 3 — compensarSaga():
 *   → Se activa cuando:
 *     a) ms-pagos publica pago.fallido
 *     b) El scheduler detecta que la reserva expiró (10 minutos sin pago)
 *   → Cambiar estado a CANCELADO o EXPIRADO según el motivo
 *   → Publicar reserva.expirada a Kafka
 *   → ms-inventario consume ese evento y libera los asientos
 */
@Service
public class ReservaSagaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaSagaService.class);

    /**
     * PASO 1: Iniciar la saga de reserva.
     * Se llama cuando el usuario hace POST /api/reservas.
     */
    public void iniciarSaga(UUID usuarioId, UUID eventoId, String claveIdempotencia) {
        log.info("SAGA INICIAR - usuario: {}, evento: {}", usuarioId, eventoId);
        // TODO: Implementar en Día 2
        // 1. Verificar evento ACTIVO en ms-eventos
        // 2. Reservar asientos en ms-inventario
        // 3. Calcular precio total
        // 4. Crear reserva en BD con estado PENDIENTE
        // 5. SET reserva:ttl:{reservaId} "activa" EX 600 en Redis
        // 6. Publicar reserva.creada a Kafka
        throw new UnsupportedOperationException("Pendiente implementar en Dia 2");
    }

    /**
     * PASO 2: Confirmar la saga cuando el pago fue exitoso.
     * Se llama cuando se consume pago.confirmado de Kafka.
     */
    public void confirmarSaga(UUID reservaId) {
        log.info("SAGA CONFIRMAR - reserva: {}", reservaId);
        // TODO: Implementar en Día 2
        // 1. Buscar reserva por ID
        // 2. Cambiar estado PENDIENTE → CONFIRMADO
        // 3. Eliminar clave TTL de Redis
        // 4. Publicar reserva.confirmada a Kafka
        throw new UnsupportedOperationException("Pendiente implementar en Dia 2");
    }

    /**
     * PASO 3: Compensar la saga cuando algo falla.
     * Se llama cuando llega pago.fallido o cuando el scheduler
     * detecta que la reserva expiró sin pago.
     *
     * @param motivo "PAGO_FALLIDO" o "TTL_EXPIRADO"
     */
    public void compensarSaga(UUID reservaId, String motivo) {
        log.info("SAGA COMPENSAR - reserva: {}, motivo: {}", reservaId, motivo);
        // TODO: Implementar en Día 2
        // 1. Buscar reserva por ID
        // 2. Cambiar estado a CANCELADO o EXPIRADO según motivo
        // 3. Publicar reserva.expirada a Kafka
        //    (ms-inventario lo consumirá y liberará los asientos)
        throw new UnsupportedOperationException("Pendiente implementar en Dia 2");
    }
}
