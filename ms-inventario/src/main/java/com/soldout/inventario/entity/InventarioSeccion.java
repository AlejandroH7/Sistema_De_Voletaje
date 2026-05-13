package com.soldout.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventario_secciones")
public class InventarioSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "seccion_id", nullable = false, unique = true)
    private Seccion seccion;

    @Column(name = "total_asientos", nullable = false)
    private Integer totalAsientos;

    @Column(name = "asientos_disponibles", nullable = false)
    private Integer asientosDisponibles;

    @Column(name = "asientos_reservados", nullable = false)
    private Integer asientosReservados;

    @Column(name = "asientos_vendidos", nullable = false)
    private Integer asientosVendidos;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    public void prePersist() {
        if (actualizadoEn == null) {
            actualizadoEn = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

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

    public Integer getTotalAsientos() {
        return totalAsientos;
    }

    public void setTotalAsientos(Integer totalAsientos) {
        this.totalAsientos = totalAsientos;
    }

    public Integer getAsientosDisponibles() {
        return asientosDisponibles;
    }

    public void setAsientosDisponibles(Integer asientosDisponibles) {
        this.asientosDisponibles = asientosDisponibles;
    }

    public Integer getAsientosReservados() {
        return asientosReservados;
    }

    public void setAsientosReservados(Integer asientosReservados) {
        this.asientosReservados = asientosReservados;
    }

    public Integer getAsientosVendidos() {
        return asientosVendidos;
    }

    public void setAsientosVendidos(Integer asientosVendidos) {
        this.asientosVendidos = asientosVendidos;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
