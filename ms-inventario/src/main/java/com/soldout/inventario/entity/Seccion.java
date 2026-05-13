package com.soldout.inventario.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "secciones")
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @OneToOne(mappedBy = "seccion", cascade = CascadeType.ALL)
    private InventarioSeccion inventarioSeccion;

    @OneToMany(mappedBy = "seccion")
    private List<Mesa> mesas = new ArrayList<>();

    @OneToMany(mappedBy = "seccion")
    private List<Asiento> asientos = new ArrayList<>();

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

    public UUID getEventoId() {
        return eventoId;
    }

    public void setEventoId(UUID eventoId) {
        this.eventoId = eventoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidadTotal() {
        return capacidadTotal;
    }

    public void setCapacidadTotal(Integer capacidadTotal) {
        this.capacidadTotal = capacidadTotal;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public InventarioSeccion getInventarioSeccion() {
        return inventarioSeccion;
    }

    public void setInventarioSeccion(InventarioSeccion inventarioSeccion) {
        this.inventarioSeccion = inventarioSeccion;
    }

    public List<Mesa> getMesas() {
        return mesas;
    }

    public void setMesas(List<Mesa> mesas) {
        this.mesas = mesas;
    }

    public List<Asiento> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<Asiento> asientos) {
        this.asientos = asientos;
    }
}
