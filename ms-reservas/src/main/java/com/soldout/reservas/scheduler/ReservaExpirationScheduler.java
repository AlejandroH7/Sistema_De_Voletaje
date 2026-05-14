package com.soldout.reservas.scheduler;

import com.soldout.reservas.entity.Reserva;
import com.soldout.reservas.repository.ReservaRepository;
import com.soldout.reservas.service.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservaExpirationScheduler {

    private static final Logger log =
        LoggerFactory.getLogger(ReservaExpirationScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;

    public ReservaExpirationScheduler(ReservaRepository reservaRepository,
                                       ReservaService reservaService) {
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
    }

    @Scheduled(fixedDelay = 60000)
    public void procesarReservasExpiradas() {
        log.info("SCHEDULER - Verificando reservas expiradas...");
        List<Reserva> expiradas = reservaRepository
            .findByEstadoAndExpiraEnBefore("PENDIENTE", LocalDateTime.now());
        for (Reserva reserva : expiradas) {
            reservaService.expirarReserva(reserva.getId());
        }
        if (!expiradas.isEmpty()) {
            log.info("SCHEDULER - {} reservas expiradas procesadas", expiradas.size());
        }
    }
}
