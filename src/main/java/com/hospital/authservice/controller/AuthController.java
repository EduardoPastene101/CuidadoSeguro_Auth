package com.hospital.authservice.controller;

import com.hospital.authservice.dto.*;
import com.hospital.authservice.entity.Paciente;
import com.hospital.authservice.service.AuthService;
import com.hospital.authservice.service.AuthServiceWithCircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse; // SOLO Swagger
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Gestión de autenticación y tokens JWT")
public class AuthController {

    private final AuthService authService;
    private final AuthServiceWithCircuitBreaker authServiceWithCircuitBreaker;

    // ===================== LOGIN =====================

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica usuario y retorna JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login attempt: {}", request.getUsername());

        AuthResponse response = authServiceWithCircuitBreaker.loginWithCircuitBreaker(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/userinfo")
    public Paciente obtenerDatosUsuario(@RequestHeader("Authorization") String token) {
        
        return authService.obtenerDetallesUsuario(token);
    }
    
    // ===================== REGISTER =====================

    @PostMapping("/register")
    @Operation(summary = "Registro", description = "Crea un usuario nuevo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        System.out.println("ENTRÓ AL REGISTER");
        log.info("Register attempt: {}", request.getUsername());

        AuthResponse response = authServiceWithCircuitBreaker.registerWithCircuitBreaker(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    

    // ===================== REFRESH =====================

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Renueva access token")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        AuthResponse response = authServiceWithCircuitBreaker.refreshTokenWithCircuitBreaker(request);
        return ResponseEntity.ok(response);
    }

    // ===================== LOGOUT =====================

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida tokens")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        authServiceWithCircuitBreaker.logoutWithCircuitBreaker(request);

        return ResponseEntity.ok(
                ApiResponseDto.success(null, "Logout exitoso")
        );
    }

    // ===================== VALIDATE =====================

    @GetMapping("/validate")
    public ResponseEntity<ApiResponseDto<Boolean>> validate(
            @RequestHeader("Authorization") String authHeader) {


        String token = authHeader;

        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }
        boolean valido = authService.validateToken(token);

        if (!valido) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponseDto.error("Token inválido o expirado")
            );

            /*
            return ResponseEntity.ok().body(
                    ApiResponseDto.error("Token inválido o expirado")
            );
             */
        }
        return ResponseEntity.ok(
                ApiResponseDto.success(valido, "Token válido")
        );
    }

    // ===================== HEALTH =====================

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<ApiResponseDto<String>> health() {

        return ResponseEntity.ok(
                ApiResponseDto.success("OK", "Auth service running")
        );
    }
}