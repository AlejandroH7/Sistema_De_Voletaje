package com.soldout.reservas.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que detecta reservas expiradas cada 60 segundos.
 *
 * Cuando un usuario crea una reserva tiene 10 minutos para pagar.
 * Si no paga, este scheduler detecta la expiración y llama al
 * orquestador de Saga para compensar (liberar asientos).
 *
 * Requiere @EnableScheduling en MsReservasApplication (ya está).
 */
@Component
public class ReservaExpirationScheduler {

    private static final Logger log =
        LoggerFactory.getLogger(ReservaExpirationScheduler.class);

    /**
     * Se ejecuta cada 60 segundos.
     * Busca reservas PENDIENTE con expira_en < NOW() y las cancela.
     */
    @Scheduled(fixedDelay = 60000)
    public void procesarReservasExpiradas() {
        log.info("SCHEDULER - Verificando reservas expiradas...");
        // TODO: Implementar en Día 2
        // 1. Buscar: findByEstadoAndExpiraEnBefore("PENDIENTE", LocalDateTime.now())
        // 2. Para cada reserva expirada:
        //    sagaService.compensarSaga(reserva.getId(), "TTL_EXPIRADO")
    }
}
