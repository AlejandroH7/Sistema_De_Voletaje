package com.soldout.eventos.controller;

import com.soldout.eventos.dto.CrearEventoRequest;
import com.soldout.eventos.dto.CrearLugarRequest;
import com.soldout.eventos.dto.RespuestaApi;
import com.soldout.eventos.entity.Evento;
import com.soldout.eventos.entity.Lugar;
import com.soldout.eventos.service.EventoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping("/api/lugares")
    public ResponseEntity<RespuestaApi<Lugar>> crearLugar(
            @Valid @RequestBody CrearLugarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.exito("Lugar creado",
                        eventoService.crearLugar(request)));
    }

    @PostMapping("/api/eventos")
    public ResponseEntity<RespuestaApi<Evento>> crearEvento(
            @Valid @RequestBody CrearEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.exito("Evento creado",
                        eventoService.crearEvento(request)));
    }

    @GetMapping("/api/eventos")
    public ResponseEntity<RespuestaApi<List<Evento>>> listarActivos() {
        return ResponseEntity.ok(
                RespuestaApi.exito("Eventos activos",
                        eventoService.listarActivos()));
    }

    @GetMapping("/api/eventos/{id}")
    public ResponseEntity<RespuestaApi<Evento>> obtenerPorId(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                RespuestaApi.exito("Evento encontrado",
                        eventoService.obtenerPorId(id)));
    }

    @PutMapping("/api/eventos/{id}/publicar")
    public ResponseEntity<RespuestaApi<Evento>> publicar(
            @PathVariable UUID id,
            @RequestBody(required = false) CrearEventoRequest request) {
        List<CrearEventoRequest.SeccionRequest> secciones =
                request != null ? request.getSecciones() : null;
        return ResponseEntity.ok(
                RespuestaApi.exito("Evento publicado",
                        eventoService.publicarEvento(id, secciones)));
    }
}
