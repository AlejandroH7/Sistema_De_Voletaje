package com.soldout.inventario.exception;

import com.soldout.inventario.dto.RespuestaApi;
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
            .body(RespuestaApi.error("Error interno del servicio", "ERROR_INTERNO"));
    }
}
