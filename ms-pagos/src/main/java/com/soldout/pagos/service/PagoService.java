package com.soldout.pagos.service;

import com.soldout.pagos.dto.CrearPagoRequest;
import com.soldout.pagos.dto.PagoResponse;
import com.soldout.pagos.entity.Pago;
import com.soldout.pagos.exception.NegocioException;
import com.soldout.pagos.repository.PagoRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PagoService {

    private static final int LONGITUD_MAXIMA_CLAVE_IDEMPOTENCIA = 64;

    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public PagoResponse crearPago(CrearPagoRequest request, String claveIdempotencia, String usuarioIdHeader) {
        String claveNormalizada = normalizarClaveIdempotencia(claveIdempotencia);
        UUID usuarioId = parsearUsuarioId(usuarioIdHeader);

        return pagoRepository.findByClaveIdempotencia(claveNormalizada)
            .map(this::toResponse)
            .orElseGet(() -> crearPagoPendiente(request, claveNormalizada, usuarioId));
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(UUID id) {
        Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new NegocioException("Pago no encontrado", "PAGO_NO_ENCONTRADO", HttpStatus.NOT_FOUND));
        return toResponse(pago);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPorReserva(UUID reservaId) {
        Pago pago = pagoRepository.findByReservaId(reservaId)
            .orElseThrow(() -> new NegocioException("Pago no encontrado", "PAGO_NO_ENCONTRADO", HttpStatus.NOT_FOUND));
        return toResponse(pago);
    }

    private PagoResponse crearPagoPendiente(CrearPagoRequest request, String claveIdempotencia, UUID usuarioId) {
        Pago pago = new Pago();
        pago.setReservaId(request.reservaId());
        pago.setUsuarioId(usuarioId);
        pago.setMonto(request.monto());
        pago.setMoneda(valorOInicial(request.moneda(), "GTQ").toUpperCase(Locale.ROOT));
        pago.setMetodoPago(valorOInicial(request.metodoPago(), "TARJETA").toUpperCase(Locale.ROOT));
        pago.setEstado("PENDIENTE");
        pago.setClaveIdempotencia(claveIdempotencia);
        return toResponse(pagoRepository.save(pago));
    }

    private String normalizarClaveIdempotencia(String claveIdempotencia) {
        if (!StringUtils.hasText(claveIdempotencia)) {
            throw new NegocioException("Idempotency-Key es obligatorio", "IDEMPOTENCY_KEY_OBLIGATORIO", HttpStatus.BAD_REQUEST);
        }

        String clave = claveIdempotencia.trim();
        if (clave.length() > LONGITUD_MAXIMA_CLAVE_IDEMPOTENCIA) {
            throw new NegocioException("Idempotency-Key no puede exceder 64 caracteres", "IDEMPOTENCY_KEY_INVALIDO", HttpStatus.BAD_REQUEST);
        }
        return clave;
    }

    private UUID parsearUsuarioId(String usuarioIdHeader) {
        if (!StringUtils.hasText(usuarioIdHeader)) {
            throw new NegocioException("X-Usuario-Id es obligatorio", "USUARIO_ID_OBLIGATORIO", HttpStatus.BAD_REQUEST);
        }

        try {
            return UUID.fromString(usuarioIdHeader.trim());
        } catch (IllegalArgumentException exception) {
            throw new NegocioException("X-Usuario-Id debe ser un UUID valido", "USUARIO_ID_INVALIDO", HttpStatus.BAD_REQUEST);
        }
    }

    private String valorOInicial(String valor, String inicial) {
        if (StringUtils.hasText(valor)) {
            return valor.trim();
        }
        return inicial;
    }

    private PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
            pago.getId(),
            pago.getReservaId(),
            pago.getUsuarioId(),
            pago.getMonto(),
            pago.getMoneda(),
            pago.getEstado(),
            pago.getMetodoPago(),
            pago.getProcesadoEn(),
            pago.getCreadoEn()
        );
    }
}
