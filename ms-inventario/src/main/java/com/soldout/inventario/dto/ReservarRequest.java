package com.soldout.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReservarRequest {
    @NotNull(message = "reservaId es obligatorio")
    private UUID reservaId;

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private int cantidad;

    public UUID getReservaId() { return reservaId; }
    public void setReservaId(UUID reservaId) { this.reservaId = reservaId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
