package com.soldout.reservas.service;

import com.soldout.reservas.dto.CrearReservaRequest;
import com.soldout.reservas.entity.DetalleReserva;
import com.soldout.reservas.entity.Reserva;
import com.soldout.reservas.exception.NegocioException;
import com.soldout.reservas.kafka.ReservaProducer;
import com.soldout.reservas.repository.DetalleReservaRepository;
import com.soldout.reservas.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);
    private static final String URL_INVENTARIO = "http://ms-inventario:8083";

    private final ReservaRepository reservaRepository;
    private final DetalleReservaRepository detalleRepo;
    private final ReservaProducer reservaProducer;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${reserva.ttl-segundos:600}")
    private long ttlSegundos;

    public ReservaService(ReservaRepository reservaRepository,
                          DetalleReservaRepository detalleRepo,
                          ReservaProducer reservaProducer,
                          RestTemplate restTemplate,
                          RedisTemplate<String, String> redisTemplate) {
        this.reservaRepository = reservaRepository;
        this.detalleRepo = detalleRepo;
        this.reservaProducer = reservaProducer;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Reserva crearReserva(UUID usuarioId, CrearReservaRequest request,
                                 String claveIdempotencia) {
        log.info("CREAR RESERVA - usuario: {}, evento: {}",
            usuarioId, request.getEventoId());

        // IDEMPOTENCIA
        if (reservaRepository.existsByClaveIdempotencia(claveIdempotencia)) {
            log.info("RESERVA DUPLICADA - retornando existente: {}", claveIdempotencia);
            return reservaRepository.findByClaveIdempotencia(claveIdempotencia)
                .orElseThrow(() -> new NegocioException(
                    "Error de idempotencia", "ERROR_INTERNO",
                    HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // Reservar asientos en ms-inventario via REST
        for (CrearReservaRequest.ItemReserva item : request.getItems()) {
            try {
                String url = URL_INVENTARIO + "/api/inventario/" +
                    item.getSeccionId() + "/reservar";

                org.springframework.http.HttpHeaders headers =
                    new org.springframework.http.HttpHeaders();
                headers.setContentType(
                    org.springframework.http.MediaType.APPLICATION_JSON);

                String body = "{\"reservaId\":\"" +
                    java.util.UUID.randomUUID() + "\",\"cantidad\":" +
                    item.getCantidad() + "}";

                org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);

                restTemplate.exchange(url,
                    org.springframework.http.HttpMethod.POST,
                    entity, String.class);

                log.info("INVENTARIO RESERVADO - seccion: {}, cantidad: {}",
                    item.getSeccionId(), item.getCantidad());
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("ERROR reservando en inventario: {}", e.getMessage());
                throw new NegocioException(
                    "No hay suficientes asientos disponibles",
                    "ASIENTOS_NO_DISPONIBLES",
                    HttpStatus.CONFLICT
                );
            } catch (Exception e) {
                log.error("ERROR llamando a inventario: {}", e.getMessage());
                throw new NegocioException(
                    "Error al comunicarse con el servicio de inventario",
                    "ERROR_INVENTARIO",
                    HttpStatus.SERVICE_UNAVAILABLE
                );
            }
        }

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(usuarioId);
        reserva.setEventoId(request.getEventoId());
        BigDecimal precioTotal = BigDecimal.ZERO;
        for (CrearReservaRequest.ItemReserva item : request.getItems()) {
            BigDecimal precioUnitario = obtenerPrecioSeccion(item.getSeccionId());
            precioTotal = precioTotal.add(
                precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()))
            );
        }
        reserva.setPrecioTotal(precioTotal);
        reserva.setEstado("PENDIENTE");
        reserva.setClaveIdempotencia(claveIdempotencia);
        reserva.setExpiraEn(LocalDateTime.now().plusSeconds(ttlSegundos));

        Reserva guardada = reservaRepository.save(reserva);

        // Guardar detalles
        for (CrearReservaRequest.ItemReserva item : request.getItems()) {
            DetalleReserva detalle = new DetalleReserva();
            detalle.setReserva(guardada);
            detalle.setSeccionId(item.getSeccionId());
            detalle.setTipoAsiento(item.getTipoAsiento() != null
                ? item.getTipoAsiento() : "GENERAL");
            detalle.setCantidad(item.getCantidad());
            detalle.setNombreSeccion("Seccion " + item.getSeccionId());
            detalle.setPrecioUnitario(obtenerPrecioSeccion(item.getSeccionId()));
            detalleRepo.save(detalle);
        }

        // Activar TTL en Redis
        redisTemplate.opsForValue().set(
            "reserva:ttl:" + guardada.getId(),
            "activa",
            ttlSegundos,
            TimeUnit.SECONDS
        );
        log.info("TTL REDIS ACTIVADO - reserva: {} por {} segundos",
            guardada.getId(), ttlSegundos);

        // Publicar a Kafka
        reservaProducer.publicarReservaCreada(guardada);

        return guardada;
    }

    private BigDecimal obtenerPrecioSeccion(UUID seccionId) {
        try {
            String url = URL_INVENTARIO + "/api/inventario/" + seccionId + "/secciones";
            String respuesta = restTemplate.getForObject(url, String.class);
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode nodo = mapper.readTree(respuesta);
            com.fasterxml.jackson.databind.JsonNode datos = nodo.get("datos");
            if (datos != null && datos.isArray() && datos.size() > 0) {
                return new BigDecimal(datos.get(0).get("seccion").get("precio").asText());
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener precio de seccion {}: {}", seccionId, e.getMessage());
        }
        return BigDecimal.valueOf(100);
    }

    @Transactional(readOnly = true)
    public Reserva obtenerPorId(UUID id) {
        return reservaRepository.findById(id)
            .orElseThrow(() -> new NegocioException(
                "Reserva no encontrada",
                "RESERVA_NO_ENCONTRADA", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Reserva> obtenerPorUsuario(UUID usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId);
    }

    @Transactional
    public void confirmarReserva(UUID reservaId) {
        Reserva reserva = obtenerPorId(reservaId);
        if (!"PENDIENTE".equals(reserva.getEstado())) return;
        reserva.setEstado("CONFIRMADO");
        reserva.setConfirmadoEn(LocalDateTime.now());
        reservaRepository.save(reserva);
        redisTemplate.delete("reserva:ttl:" + reservaId);
        reservaProducer.publicarReservaConfirmada(reserva);
        log.info("RESERVA CONFIRMADA: {}", reservaId);
    }

    @Transactional
    public void expirarReserva(UUID reservaId) {
        Reserva reserva = obtenerPorId(reservaId);
        if (!"PENDIENTE".equals(reserva.getEstado())) return;
        reserva.setEstado("EXPIRADO");
        reservaRepository.save(reserva);
        reservaProducer.publicarReservaExpirada(reserva);
        log.info("RESERVA EXPIRADA: {}", reservaId);
    }
}
