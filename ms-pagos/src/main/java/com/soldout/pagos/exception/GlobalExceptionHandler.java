package com.soldout.pagos.exception;

import com.soldout.pagos.dto.RespuestaApi;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<RespuestaApi<Void>> manejarNegocio(NegocioException exception) {
        return ResponseEntity
            .status(exception.getEstadoHttp())
            .body(RespuestaApi.error(exception.getMessage(), exception.getCodigoError()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaApi<Void>> manejarValidacion(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(RespuestaApi.error(mensaje, "SOLICITUD_INVALIDA"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<RespuestaApi<Void>> manejarHeaderFaltante(MissingRequestHeaderException exception) {
        return ResponseEntity.badRequest()
            .body(RespuestaApi.error("Header obligatorio faltante: " + exception.getHeaderName(), "HEADER_OBLIGATORIO"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaApi<Void>> manejarArgumentoInvalido(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(RespuestaApi.error(exception.getMessage(), "SOLICITUD_INVALIDA"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApi<Void>> manejarGeneral(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RespuestaApi.error("Error interno del servicio", "ERROR_INTERNO"));
    }
}
