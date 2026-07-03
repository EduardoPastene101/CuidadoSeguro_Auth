package com.hospital.authservice.exception;

import com.hospital.authservice.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("Datos inválidos");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(message, "BAD_REQUEST"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ex.getMessage(), "BAD_REQUEST"));
    }

    /**
     * Errores de negocio propios del dominio de auth (username en uso,
     * credenciales inválidas, token inválido, etc). Estos SÍ son 401/400
     * legítimos, no fallos de infraestructura.
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAuthException(AuthException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(ex.getMessage(), "UNAUTHORIZED"));
    }

    /**
     * Fallos al llamar a otro microservicio (paciente-ms, dm-ms, etc.),
     * lanzados explícitamente por los servicios como DownstreamServiceException,
     * o capturados directamente aquí si algún RestTemplate no fue envuelto en
     * try/catch en el código de negocio. En ningún caso deben mapearse a 401:
     * el problema es de disponibilidad de otro servicio, no de autenticación.
     */
    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleDownstreamServiceException(
            DownstreamServiceException ex) {

        log.error("Fallo de servicio downstream '{}': {}", ex.getServicioDestino(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDto.error(ex.getMessage(), "DOWNSTREAM_SERVICE_UNAVAILABLE"));
    }

    @ExceptionHandler({RestClientException.class, ResourceAccessException.class})
    public ResponseEntity<ApiResponseDto<Void>> handleRestClientException(RestClientException ex) {

        log.error("Fallo de comunicación con un servicio externo no capturado explícitamente: {}",
                ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDto.error(
                        "Un servicio dependiente no está disponible en este momento. Intente más tarde.",
                        "DOWNSTREAM_SERVICE_UNAVAILABLE"));
    }

    /**
     * Último recurso: cualquier otra excepción no anticipada. Se registra
     * completa en el log y se devuelve 500, en vez de disfrazarla de 401.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleRuntimeException(
            RuntimeException ex) {

        log.error("Error interno no controlado", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                        "Error interno del servidor",
                        "INTERNAL_ERROR"));
    }
}