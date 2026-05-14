package com.soldout.reservas.exception;

import com.soldout.reservas.dto.RespuestaApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<RespuestaApi<Void>> manejarNegocio(NegocioException ex) {
        return ResponseEntity.status(ex.getEstadoHttp())
            .body(RespuestaApi.error(ex.getMessage(), ex.getCodigoError()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApi<Void>> manejarGeneral(Exception ex) {
        return ResponseEntity.internalServerError()
            .body(RespuestaApi.error("Error interno", "ERROR_INTERNO"));
    }
}
