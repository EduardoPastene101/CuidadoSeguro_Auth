package com.hospital.authservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // Sin estos timeouts, el RestTemplate por defecto puede esperar
    // indefinidamente si paciente-ms o dm-ms no responden (conexión colgada
    // en vez de un error rápido y claro), bloqueando el hilo de la transacción
    // de registro.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}