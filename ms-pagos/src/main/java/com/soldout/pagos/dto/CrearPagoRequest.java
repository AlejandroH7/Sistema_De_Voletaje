package com.soldout.pagos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearPagoRequest(
    @NotNull
    @JsonProperty("reserva_id")
    UUID reservaId,

    @NotNull
    @DecimalMin(value = "0.01", message = "debe ser mayor a 0")
    BigDecimal monto,

    @Pattern(regexp = "^[A-Z]{3}$", message = "debe tener 3 letras mayusculas")
    String moneda,

    @Size(max = 50)
    @JsonProperty("metodo_pago")
    String metodoPago
) {
}
