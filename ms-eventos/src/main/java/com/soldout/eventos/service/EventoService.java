package com.soldout.eventos.service;

import com.soldout.eventos.dto.CrearEventoRequest;
import com.soldout.eventos.dto.CrearLugarRequest;
import com.soldout.eventos.entity.Evento;
import com.soldout.eventos.entity.Lugar;
import com.soldout.eventos.exception.NegocioException;
import com.soldout.eventos.kafka.EventoProducer;
import com.soldout.eventos.repository.EventoRepository;
import com.soldout.eventos.repository.LugarRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final LugarRepository lugarRepository;
    private final EventoProducer eventoProducer;

    public EventoService(
            EventoRepository eventoRepository,
            LugarRepository lugarRepository,
            EventoProducer eventoProducer) {
        this.eventoRepository = eventoRepository;
        this.lugarRepository = lugarRepository;
        this.eventoProducer = eventoProducer;
    }

    @Transactional
    public Lugar crearLugar(CrearLugarRequest request) {
        Lugar lugar = new Lugar();
        lugar.setNombre(request.getNombre());
        lugar.setDireccion(request.getDireccion());
        lugar.setCiudad(request.getCiudad());
        lugar.setCapacidadMaxima(request.getCapacidadMaxima());
        return lugarRepository.save(lugar);
    }

    @Transactional
    public Evento crearEvento(CrearEventoRequest request) {
        Lugar lugar = lugarRepository.findById(request.getLugarId())
                .orElseThrow(() -> new NegocioException(
                        "Lugar no encontrado",
                        "LUGAR_NO_ENCONTRADO",
                        HttpStatus.NOT_FOUND));

        Evento evento = new Evento();
        evento.setNombre(request.getNombre());
        evento.setDescripcion(request.getDescripcion());
        evento.setLugar(lugar);
        evento.setFechaEvento(request.getFechaEvento());
        evento.setTipoEvento(request.getTipoEvento());
        evento.setEstado("BORRADOR");
        return eventoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public List<Evento> listarActivos() {
        return eventoRepository.findByEstado("ACTIVO");
    }

    @Transactional(readOnly = true)
    public Evento obtenerPorId(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new NegocioException(
                        "Evento no encontrado",
                        "EVENTO_NO_ENCONTRADO",
                        HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Evento publicarEvento(
            UUID id,
            List<CrearEventoRequest.SeccionRequest> secciones) {
        Evento evento = obtenerPorId(id);
        if (!"BORRADOR".equals(evento.getEstado())) {
            throw new NegocioException(
                    "Solo se pueden publicar eventos en estado BORRADOR",
                    "EVENTO_NO_PUBLICABLE",
                    HttpStatus.BAD_REQUEST);
        }
        evento.setEstado("ACTIVO");
        Evento guardado = eventoRepository.save(evento);
        eventoProducer.publicarEventoCreado(guardado, secciones);
        return guardado;
    }
}
