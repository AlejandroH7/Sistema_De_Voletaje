package com.soldout.inventario.service;

import com.soldout.inventario.entity.InventarioSeccion;
import com.soldout.inventario.entity.Seccion;
import com.soldout.inventario.exception.NegocioException;
import com.soldout.inventario.repository.InventarioSeccionRepository;
import com.soldout.inventario.repository.SeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final InventarioSeccionRepository inventarioRepo;
    private final SeccionRepository seccionRepo;
    private final InventarioCacheService cacheService;

    public InventarioService(InventarioSeccionRepository inventarioRepo,
                             SeccionRepository seccionRepo,
                             InventarioCacheService cacheService) {
        this.inventarioRepo = inventarioRepo;
        this.seccionRepo = seccionRepo;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public List<InventarioSeccion> obtenerDisponibilidadPorEvento(UUID eventoId) {
        List<Seccion> secciones = seccionRepo.findByEventoId(eventoId);
        if (secciones.isEmpty()) {
            throw new NegocioException(
                "No se encontraron secciones para el evento",
                "SECCION_NO_ENCONTRADA",
                HttpStatus.NOT_FOUND
            );
        }

        return secciones.stream().map(seccion -> {
            Integer disponiblesCache = cacheService.obtenerDisponibilidad(seccion.getId());

            InventarioSeccion inv = inventarioRepo.findBySeccionId(seccion.getId())
                .orElseThrow(() -> new NegocioException(
                    "Inventario no inicializado para la seccion",
                    "INVENTARIO_NO_INICIALIZADO",
                    HttpStatus.INTERNAL_SERVER_ERROR
                ));

            if (disponiblesCache == null) {
                log.info("CACHE MISS - guardando en Redis: seccion: {}", seccion.getId());
                cacheService.guardarDisponibilidad(seccion.getId(),
                    inv.getAsientosDisponibles());
            }

            return inv;
        }).toList();
    }

    @Transactional
    public InventarioSeccion reservarAsientos(UUID seccionId, int cantidad) {
        log.info("RESERVA ATOMICA - seccion: {}, cantidad: {}", seccionId, cantidad);

        int filasActualizadas = inventarioRepo.reservarAsientos(seccionId, cantidad);

        if (filasActualizadas == 0) {
            log.warn("RESERVA RECHAZADA - sin disponibilidad: seccion: {}", seccionId);
            throw new NegocioException(
                "No hay suficientes asientos disponibles",
                "ASIENTOS_NO_DISPONIBLES",
                HttpStatus.CONFLICT
            );
        }

        cacheService.invalidarDisponibilidad(seccionId);
        log.info("RESERVA EXITOSA - seccion: {}", seccionId);

        return inventarioRepo.findBySeccionId(seccionId)
            .orElseThrow(() -> new NegocioException(
                "Error al obtener inventario actualizado",
                "ERROR_INTERNO",
                HttpStatus.INTERNAL_SERVER_ERROR
            ));
    }

    @Transactional
    public void liberarAsientos(UUID seccionId, int cantidad) {
        log.info("LIBERAR ASIENTOS - seccion: {}, cantidad: {}", seccionId, cantidad);
        inventarioRepo.liberarAsientos(seccionId, cantidad);
        cacheService.invalidarDisponibilidad(seccionId);
    }
}
