package com.soldout.pagos.controller;

import com.soldout.pagos.dto.CrearPagoRequest;
import com.soldout.pagos.dto.PagoResponse;
import com.soldout.pagos.dto.RespuestaApi;
import com.soldout.pagos.service.PagoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApi<PagoResponse>> crear(
        @Valid @RequestBody CrearPagoRequest request,
        @RequestHeader("Idempotency-Key") String claveIdempotencia,
        @RequestHeader("X-Usuario-Id") String usuarioId
    ) {
        PagoResponse pago = pagoService.crearPago(request, claveIdempotencia, usuarioId);
        HttpStatus estado = "PENDIENTE".equals(pago.estado()) ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(RespuestaApi.exito("Pago pendiente", pago));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApi<PagoResponse>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(RespuestaApi.exito("Pago encontrado", pagoService.obtenerPorId(id)));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<RespuestaApi<PagoResponse>> obtenerPorReserva(@PathVariable UUID reservaId) {
        return ResponseEntity.ok(RespuestaApi.exito("Pago encontrado", pagoService.obtenerPorReserva(reservaId)));
    }
}
