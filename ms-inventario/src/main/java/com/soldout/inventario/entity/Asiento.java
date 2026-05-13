package com.soldout.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asientos")
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @Column(name = "numero_asiento", nullable = false, length = 20)
    private String numeroAsiento;

    @Column(name = "fila", length = 10)
    private String fila;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "reserva_id")
    private UUID reservaId;

    @Column(name = "reservado_en")
    private LocalDateTime reservadoEn;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public void setSeccion(Seccion seccion) {
        this.seccion = seccion;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public String getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(String numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public void setReservaId(UUID reservaId) {
        this.reservaId = reservaId;
    }

    public LocalDateTime getReservadoEn() {
        return reservadoEn;
    }

    public void setReservadoEn(LocalDateTime reservadoEn) {
        this.reservadoEn = reservadoEn;
    }
}
