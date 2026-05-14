package com.soldout.reservas.controller;

import com.soldout.reservas.dto.CrearReservaRequest;
import com.soldout.reservas.dto.RespuestaApi;
import com.soldout.reservas.entity.Reserva;
import com.soldout.reservas.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<RespuestaApi<Reserva>> crear(
            @RequestHeader("X-Usuario-Id") UUID usuarioId,
            @RequestHeader(value = "Idempotency-Key",
                           required = false, defaultValue = "") String claveIdempotencia,
            @Valid @RequestBody CrearReservaRequest request) {
        if (claveIdempotencia.isBlank()) {
            claveIdempotencia = UUID.randomUUID().toString();
        }
        Reserva reserva = reservaService.crearReserva(
            usuarioId, request, claveIdempotencia);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RespuestaApi.exito("Reserva creada", reserva));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaApi<Reserva>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(
            RespuestaApi.exito("Reserva encontrada",
                reservaService.obtenerPorId(id)));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<RespuestaApi<List<Reserva>>> obtenerPorUsuario(
            @PathVariable UUID usuarioId) {
        return ResponseEntity.ok(
            RespuestaApi.exito("Reservas del usuario",
                reservaService.obtenerPorUsuario(usuarioId)));
    }
}
