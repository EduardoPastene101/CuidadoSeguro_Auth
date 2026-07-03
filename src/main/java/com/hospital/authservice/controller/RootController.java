package com.hospital.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint raíz dedicado exclusivamente a satisfacer el health check por
 * defecto del ALB/Target Group de ECS (que golpea "/" cada ~30s esperando
 * un 200). Sin este endpoint, esas peticiones caían en Spring Security como
 * no autenticadas (401), lo que puede llevar a ECS a marcar la tarea como
 * no saludable y reiniciarla.
 *
 * El health check funcional real de la aplicación sigue siendo
 * GET /auth/health (ver AuthController), que valida más que solo que el
 * proceso esté arriba.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.ok().build();
    }
}