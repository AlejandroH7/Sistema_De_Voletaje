package com.soldout.reservas.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class CrearReservaRequest {
    @NotNull(message = "eventoId es obligatorio")
    private UUID eventoId;
    @NotNull(message = "Los items son obligatorios")
    private List<ItemReserva> items;

    public UUID getEventoId() { return eventoId; }
    public void setEventoId(UUID eventoId) { this.eventoId = eventoId; }
    public List<ItemReserva> getItems() { return items; }
    public void setItems(List<ItemReserva> items) { this.items = items; }

    public static class ItemReserva {
        private UUID seccionId;
        private String tipoAsiento;
        private int cantidad;
        private UUID asientoId;

        public UUID getSeccionId() { return seccionId; }
        public void setSeccionId(UUID seccionId) { this.seccionId = seccionId; }
        public String getTipoAsiento() { return tipoAsiento; }
        public void setTipoAsiento(String t) { this.tipoAsiento = t; }
        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
        public UUID getAsientoId() { return asientoId; }
        public void setAsientoId(UUID asientoId) { this.asientoId = asientoId; }
    }
}
