package com.hospital.authservice.config;

import com.hospital.authservice.exception.AuthException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class Resilience4jConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% de fallas para abrir el circuito
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Tiempo que permanece abierto
                .slidingWindowSize(10) // Ventana de 10 llamadas
                .minimumNumberOfCalls(5) // Mínimo de llamadas para calcular tasa de falla
                .permittedNumberOfCallsInHalfOpenState(3) // Llamadas permitidas en estado half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true) // Transición automática a half-open
                .recordExceptions(Exception.class) // Registrar todas las excepciones
                .ignoreExceptions(
                        IllegalArgumentException.class, // Ignorar excepciones de validación
                        AuthException.class // Errores de negocio (username en uso, credenciales, etc.)
                                             // no deben contarse como fallo de infraestructura
                )
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        // Agregar listener para eventos del circuit breaker
        CircuitBreaker circuitBreaker = registry.circuitBreaker("authCircuitBreaker");
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.info("Circuit breaker '{}' transitioned from {} to {}",
                            circuitBreaker.getName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                })
                .onFailureRateExceeded(event -> {
                    log.warn("Circuit breaker '{}' failure rate exceeded: {}%",
                            circuitBreaker.getName(),
                            event.getFailureRate());
                })
                .onCallNotPermitted(event -> {
                    log.warn("Circuit breaker '{}' call not permitted", circuitBreaker.getName());
                })
                .onError(event -> {
                    log.error("Circuit breaker '{}' error: {}", 
                            circuitBreaker.getName(),
                            event.getThrowable().getMessage());
                });
        
        return registry;
    }
}