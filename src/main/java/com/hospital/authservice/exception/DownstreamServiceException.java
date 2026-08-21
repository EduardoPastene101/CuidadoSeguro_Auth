package com.hospital.authservice.exception;

/**
 * Se lanza cuando una llamada HTTP a otro microservicio (paciente-ms, dm-ms, etc.)
 * falla por caída, timeout, o respuesta de error del servicio remoto.
 *
 * Se distingue deliberadamente de {@link AuthException} (errores de negocio,
 * ej. "username ya en uso") para que el GlobalExceptionHandler pueda:
 *  - devolver el status code correcto (502/503, no 401)
 *  - NO contarla como fallo de autenticación
 *  - permitir que Resilience4j la trate como fallo de infraestructura real
 */
public class DownstreamServiceException extends RuntimeException {

    private final String servicioDestino;

    public DownstreamServiceException(String servicioDestino, String message, Throwable cause) {
        super(message, cause);
        this.servicioDestino = servicioDestino;
    }

    public String getServicioDestino() {
        return servicioDestino;
    }
}