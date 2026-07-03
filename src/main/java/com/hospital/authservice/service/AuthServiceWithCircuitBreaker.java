package com.hospital.authservice.service;

import com.hospital.authservice.dto.*;
import com.hospital.authservice.exception.AuthException;
import com.hospital.authservice.exception.DownstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceWithCircuitBreaker {
    
    private final AuthService authService;
    
    private static final String AUTH_CIRCUIT_BREAKER = "authCircuitBreaker";
    
    @CircuitBreaker(
            name = AUTH_CIRCUIT_BREAKER,
            fallbackMethod = "fallbackLogin"
    )
    public AuthResponse loginWithCircuitBreaker(LoginRequest request) {
        log.debug("Ejecutando login con circuit breaker para usuario: {}", request.getUsername());
        return authService.login(request);
    }
    
    @CircuitBreaker(
            name = AUTH_CIRCUIT_BREAKER,
            fallbackMethod = "fallbackRegister"
    )
    public AuthResponse registerWithCircuitBreaker(RegisterRequest request) {
        log.debug("Ejecutando registro con circuit breaker para usuario: {}", request.getUsername());
        return authService.register(request);
    }
    
    @CircuitBreaker(
            name = AUTH_CIRCUIT_BREAKER,
            fallbackMethod = "fallbackRefreshToken"
    )
    public AuthResponse refreshTokenWithCircuitBreaker(RefreshRequest request) {
        log.debug("Ejecutando refresh token con circuit breaker");
        return authService.refreshToken(request);
    }
    
    @CircuitBreaker(
            name = AUTH_CIRCUIT_BREAKER,
            fallbackMethod = "fallbackLogout"
    )
    public ApiResponseDto<Void> logoutWithCircuitBreaker(LogoutRequest request) {
        log.debug("Ejecutando logout con circuit breaker");
        return authService.logout(request);
    }
    
    // ===================== Métodos fallback =====================
    //
    // IMPORTANTE: si la excepción original es un error de NEGOCIO
    // (AuthException: username en uso, credenciales inválidas, etc.;
    // IllegalArgumentException: validaciones de los factory de usuario,
    // como "el admin debe tener ROLE_ADMIN") la relanzamos tal cual — el
    // usuario debe ver ese mensaje real, no un genérico "servicio no
    // disponible". Solo cuando el fallo es de infraestructura (timeout,
    // conexión rechazada, circuito abierto, servicio caído) devolvemos
    // DownstreamServiceException, que el GlobalExceptionHandler traduce a 503.
    //
    // OJO: Resilience4j invoca el método fallback para CUALQUIER excepción
    // que lance el método real, incluso las que están en "ignoreExceptions"
    // de Resilience4jConfig (ese ignore solo afecta si cuenta o no para abrir
    // el circuito, no si dispara el fallback). Por eso este chequeo es
    // necesario aquí y no basta con la config del circuit breaker.
    //
    // Además, NUNCA devolvemos un AuthResponse "vacío" con mensaje de error:
    // eso haría que el controller responda 200/201 como si la operación
    // hubiera sido exitosa, cuando en realidad falló.

    /**
     * Si la excepción es un error de negocio/validación conocido, la relanza
     * tal cual (para que el GlobalExceptionHandler la traduzca al status
     * correcto: 401 para AuthException, 400 para IllegalArgumentException).
     * Si no, no hace nada y deja que el llamador decida cómo envolverla
     * como fallo de infraestructura.
     */
    private void rethrowIfBusinessException(Throwable ex) {
        if (ex instanceof AuthException authEx) {
            throw authEx;
        }
        if (ex instanceof IllegalArgumentException illegalArgEx) {
            throw illegalArgEx;
        }
    }

    public AuthResponse fallbackLogin(LoginRequest request, Throwable ex) {
        log.error("Fallback login para usuario {} debido a: {}", request.getUsername(), ex.getMessage());
        rethrowIfBusinessException(ex);
        throw new DownstreamServiceException(
                "auth-login",
                "El servicio de autenticación no está disponible en este momento. Intente más tarde.",
                ex);
    }

    public AuthResponse fallbackRegister(RegisterRequest request, Throwable ex) {
        log.error("Fallback register para usuario {} debido a: {}", request.getUsername(), ex.getMessage());
        rethrowIfBusinessException(ex);
        throw new DownstreamServiceException(
                "auth-register",
                "El servicio de registro no está disponible en este momento. Intente más tarde.",
                ex);
    }

    public AuthResponse fallbackRefreshToken(RefreshRequest request, Throwable ex) {
        log.error("Fallback refresh token debido a: {}", ex.getMessage());
        rethrowIfBusinessException(ex);
        throw new DownstreamServiceException(
                "auth-refresh",
                "El servicio de renovación de token no está disponible en este momento. Intente más tarde.",
                ex);
    }

    public ApiResponseDto<Void> fallbackLogout(LogoutRequest request, Throwable ex) {
        log.error("Fallback logout debido a: {}", ex.getMessage());
        rethrowIfBusinessException(ex);
        throw new DownstreamServiceException(
                "auth-logout",
                "El servicio de logout no está disponible en este momento. Intente más tarde.",
                ex);
    }
}