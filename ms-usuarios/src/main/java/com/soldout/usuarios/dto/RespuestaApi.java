package com.soldout.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaApi<T>(
    boolean exito,
    String mensaje,
    T datos,
    @JsonProperty("codigo_error")
    String codigoError,
    Instant timestamp
) {

    public static <T> RespuestaApi<T> exito(String mensaje, T datos) {
        return new RespuestaApi<>(true, mensaje, datos, null, Instant.now());
    }

    public static <T> RespuestaApi<T> error(String mensaje, String codigoError) {
        return new RespuestaApi<>(false, mensaje, null, codigoError, Instant.now());
    }
}
