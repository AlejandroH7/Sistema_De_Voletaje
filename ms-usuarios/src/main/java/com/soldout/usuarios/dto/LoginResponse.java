package com.soldout.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record LoginResponse(
    String token,
    String tipo,
    @JsonProperty("expira_en")
    Instant expiraEn,
    Object usuario
) {
}
