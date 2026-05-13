package com.soldout.pagos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoResponse(
    UUID id,
    @JsonProperty("reserva_id")
    UUID reservaId,
    @JsonProperty("usuario_id")
    UUID usuarioId,
    BigDecimal monto,
    String moneda,
    String estado,
    @JsonProperty("metodo_pago")
    String metodoPago,
    @JsonProperty("procesado_en")
    LocalDateTime procesadoEn,
    @JsonProperty("creado_en")
    LocalDateTime creadoEn
) {
}
