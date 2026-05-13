package com.soldout.notificaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "reserva_id")
    private UUID reservaId;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "canal", nullable = false, length = 20)
    private String canal;

    @Column(name = "asunto", length = 255)
    private String asunto;

    @Column(name = "contenido")
    private String contenido;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 64)
    private String claveIdempotencia;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public void setReservaId(UUID reservaId) {
        this.reservaId = reservaId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    public LocalDateTime getEnviadoEn() {
        return enviadoEn;
    }

    public void setEnviadoEn(LocalDateTime enviadoEn) {
        this.enviadoEn = enviadoEn;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
