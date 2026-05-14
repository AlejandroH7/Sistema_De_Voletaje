package com.soldout.reservas.exception;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {
    private final String codigoError;
    private final HttpStatus estadoHttp;

    public NegocioException(String mensaje, String codigoError, HttpStatus estadoHttp) {
        super(mensaje);
        this.codigoError = codigoError;
        this.estadoHttp = estadoHttp;
    }

    public String getCodigoError() { return codigoError; }
    public HttpStatus getEstadoHttp() { return estadoHttp; }
}
