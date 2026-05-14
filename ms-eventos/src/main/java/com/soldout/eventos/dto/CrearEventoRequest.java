package com.soldout.eventos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CrearEventoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El lugar es obligatorio")
    private UUID lugarId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDateTime fechaEvento;

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipoEvento;

    private List<SeccionRequest> secciones;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String d) {
        this.descripcion = d;
    }

    public UUID getLugarId() {
        return lugarId;
    }

    public void setLugarId(UUID lugarId) {
        this.lugarId = lugarId;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDateTime f) {
        this.fechaEvento = f;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String t) {
        this.tipoEvento = t;
    }

    public List<SeccionRequest> getSecciones() {
        return secciones;
    }

    public void setSecciones(List<SeccionRequest> s) {
        this.secciones = s;
    }

    public static class SeccionRequest {
        private String nombre;
        private String tipo;
        private int capacidad;
        private double precio;
        private String descripcion;

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

        public int getCapacidad() {
            return capacidad;
        }

        public void setCapacidad(int capacidad) {
            this.capacidad = capacidad;
        }

        public double getPrecio() {
            return precio;
        }

        public void setPrecio(double precio) {
            this.precio = precio;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String d) {
            this.descripcion = d;
        }
    }
}
