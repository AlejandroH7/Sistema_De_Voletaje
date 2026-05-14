package com.soldout.inventario.dto;

import java.time.Instant;

public class RespuestaApi<T> {
    private boolean exito;
    private String mensaje;
    private T datos;
    private String codigoError;
    private String timestamp;

    private RespuestaApi() { this.timestamp = Instant.now().toString(); }

    public static <T> RespuestaApi<T> exito(String mensaje, T datos) {
        RespuestaApi<T> r = new RespuestaApi<>();
        r.exito = true; r.mensaje = mensaje; r.datos = datos;
        return r;
    }

    public static <T> RespuestaApi<T> error(String mensaje, String codigoError) {
        RespuestaApi<T> r = new RespuestaApi<>();
        r.exito = false; r.mensaje = mensaje; r.codigoError = codigoError;
        return r;
    }

    public boolean isExito() { return exito; }
    public String getMensaje() { return mensaje; }
    public T getDatos() { return datos; }
    public String getCodigoError() { return codigoError; }
    public String getTimestamp() { return timestamp; }
}
