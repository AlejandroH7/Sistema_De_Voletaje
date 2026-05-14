package com.soldout.pagos.controller;

import com.soldout.pagos.dto.RespuestaApi;
import com.soldout.pagos.entity.Pago;
import com.soldout.pagos.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApi<Pago>> procesarPago(
            @RequestHeader(value = "Idempotency-Key",
                           required = false, defaultValue = "") String claveIdempotencia,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioIdHeader,
            @RequestBody java.util.Map<String, Object> body) {

        if (claveIdempotencia.isBlank()) {
            claveIdempotencia = UUID.randomUUID().toString();
        }

        UUID reservaId = UUID.fromString(body.get("reserva_id").toString());
        UUID usuarioId = usuarioIdHeader != null
            ? UUID.fromString(usuarioIdHeader)
            : UUID.fromString(body.get("usuario_id").toString());
        BigDecimal monto = new BigDecimal(body.get("monto").toString());
        String metodoPago = body.containsKey("metodo_pago")
            ? body.get("metodo_pago").toString() : "TARJETA";

        Pago pago = pagoService.procesarPago(
            reservaId, usuarioId, monto, metodoPago, claveIdempotencia);

        String mensaje = "COMPLETADO".equals(pago.getEstado())
            ? "Pago procesado exitosamente"
            : "Pago rechazado por el procesador";

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RespuestaApi.exito(mensaje, pago));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApi<Pago>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(
            RespuestaApi.exito("Pago encontrado", pagoService.obtenerPorId(id)));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<RespuestaApi<Pago>> obtenerPorReserva(
            @PathVariable UUID reservaId) {
        return ResponseEntity.ok(
            RespuestaApi.exito("Pago de la reserva",
                pagoService.obtenerPorReservaId(reservaId)));
    }
}
