package com.soldout.usuarios.exception;

import com.soldout.usuarios.dto.RespuestaApi;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<RespuestaApi<Void>> manejarTokenExpirado(ExpiredJwtException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(RespuestaApi.error("Token expirado", "TOKEN_EXPIRADO"));
    }

    @ExceptionHandler({JwtException.class, BadCredentialsException.class, AccessDeniedException.class})
    public ResponseEntity<RespuestaApi<Void>> manejarTokenInvalido(Exception exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(RespuestaApi.error("Token invalido", "TOKEN_INVALIDO"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApi<Void>> manejarGeneral(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RespuestaApi.error("Error interno del servicio", "ERROR_INTERNO"));
    }
}
