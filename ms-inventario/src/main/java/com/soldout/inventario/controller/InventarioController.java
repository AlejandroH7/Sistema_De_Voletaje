package com.soldout.inventario.controller;

import com.soldout.inventario.dto.RespuestaApi;
import com.soldout.inventario.dto.ReservarRequest;
import com.soldout.inventario.entity.InventarioSeccion;
import com.soldout.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/{eventoId}/secciones")
    public ResponseEntity<RespuestaApi<List<InventarioSeccion>>> obtenerDisponibilidad(
            @PathVariable UUID eventoId) {
        List<InventarioSeccion> inventario =
            inventarioService.obtenerDisponibilidadPorEvento(eventoId);
        return ResponseEntity.ok(
            RespuestaApi.exito("Disponibilidad obtenida", inventario));
    }

    @PostMapping("/{seccionId}/reservar")
    public ResponseEntity<RespuestaApi<InventarioSeccion>> reservar(
            @PathVariable UUID seccionId,
            @Valid @RequestBody ReservarRequest request) {
        InventarioSeccion resultado =
            inventarioService.reservarAsientos(seccionId, request.getCantidad());
        return ResponseEntity.ok(
            RespuestaApi.exito("Asientos reservados exitosamente", resultado));
    }

    @PostMapping("/{seccionId}/liberar")
    public ResponseEntity<RespuestaApi<Void>> liberar(
            @PathVariable UUID seccionId,
            @Valid @RequestBody ReservarRequest request) {
        inventarioService.liberarAsientos(seccionId, request.getCantidad());
        return ResponseEntity.ok(
            RespuestaApi.exito("Asientos liberados exitosamente", null));
    }
}
