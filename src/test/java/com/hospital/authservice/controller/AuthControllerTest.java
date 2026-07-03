package com.hospital.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.authservice.dto.*;
import com.hospital.authservice.exception.AuthException;
import com.hospital.authservice.service.AuthService;
import com.hospital.authservice.service.AuthServiceWithCircuitBreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.hospital.authservice.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        @Mock
        private AuthService authService;

        @Mock
        private AuthServiceWithCircuitBreaker authServiceWithCircuitBreaker;

        @InjectMocks
        private AuthController authController;

        private MockMvc mockMvc;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                this.mockMvc = MockMvcBuilders
                                .standaloneSetup(authController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();

                this.objectMapper = new ObjectMapper();
        }

        @Test
        void testLoginSuccess() throws Exception {
                // Given
                LoginRequest request = LoginRequest.builder()
                                .username("testuser")
                                .password("password123")
                                .build();

                AuthResponse response = AuthResponse.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .tokenType("Bearer")
                                .expiresIn(900L)
                                .message("Login exitoso")
                                .build();

                when(authServiceWithCircuitBreaker.loginWithCircuitBreaker(any(LoginRequest.class))).thenReturn(response);

                // When & Then
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-token"))
                                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.expiresIn").value(900))
                                .andExpect(jsonPath("$.message").value("Login exitoso"));
        }

        @Test
        void testLoginUnauthorized() throws Exception {
                // Given
                LoginRequest request = LoginRequest.builder()
                                .username("testuser")
                                .password("wrongpassword")
                                .build();

                when(authServiceWithCircuitBreaker.loginWithCircuitBreaker(any(LoginRequest.class)))
                                .thenThrow(new AuthException("Credenciales inválidas"));

                // When & Then
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testRegisterSuccess() throws Exception {
                // Given
                RegisterRequest request = RegisterRequest.builder()
                                .username("newuser")
                                .password("Password123!")
                                .email("newuser@test.com")
                                .tipoUsuario("PACIENTE")
                                .nombres("Juan")
                                .apellidos("Perez")
                                .tipoDocumento("DNI")
                                .numeroDocumento("12345678")
                                .aceptaTerminos(true)
                                .build();

                AuthResponse response = AuthResponse.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .tokenType("Bearer")
                                .expiresIn(900L)
                                .message("Registro exitoso")
                                .build();

                when(authServiceWithCircuitBreaker.registerWithCircuitBreaker(any(RegisterRequest.class))).thenReturn(response);

                // When & Then
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.accessToken").value("access-token"))
                                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                                .andExpect(jsonPath("$.message").value("Registro exitoso"));
        }

        @Test
        void testRegisterBadRequest() throws Exception {

                RegisterRequest request = RegisterRequest.builder()
                                .username("")
                                .password("Password123!")
                                .email("invalid-email")
                                .tipoUsuario("PACIENTE")
                                .build();

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testRefreshTokenSuccess() throws Exception {
                // Given
                RefreshRequest request = RefreshRequest.builder()
                                .refreshToken("refresh-token")
                                .build();

                AuthResponse response = AuthResponse.builder()
                                .accessToken("new-access-token")
                                .refreshToken("new-refresh-token")
                                .tokenType("Bearer")
                                .expiresIn(900L)
                                .message("Token refrescado exitosamente")
                                .build();

                when(authServiceWithCircuitBreaker.refreshTokenWithCircuitBreaker(any(RefreshRequest.class))).thenReturn(response);

                // When & Then
                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                                .andExpect(jsonPath("$.message").value("Token refrescado exitosamente"));
        }

        @Test
        void testRefreshTokenUnauthorized() throws Exception {
                // Given
                RefreshRequest request = RefreshRequest.builder()
                                .refreshToken("invalid-refresh-token")
                                .build();

                when(authServiceWithCircuitBreaker.refreshTokenWithCircuitBreaker(any(RefreshRequest.class)))
                                .thenThrow(new AuthException("Refresh token inválido"));

                // When & Then
                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testLogoutSuccess() throws Exception {
                // Given
                LogoutRequest request = LogoutRequest.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .build();

                ApiResponseDto<Void> response = ApiResponseDto.success(null, "Logout exitoso");
                when(authServiceWithCircuitBreaker.logoutWithCircuitBreaker(any(LogoutRequest.class))).thenReturn(response);

                // When & Then
                mockMvc.perform(post("/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Logout exitoso"));
        }

        @Test
        void testLogoutBadRequest() throws Exception {

                mockMvc.perform(post("/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testValidateTokenSuccess() throws Exception {

                when(authService.validateToken("valid-token"))
                                .thenReturn(true);

                mockMvc.perform(get("/auth/validate")
                                .header("Authorization", "Bearer valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").value(true))
                                .andExpect(jsonPath("$.message").value("Token válido"));
        }

        @Test
        void testValidateTokenInvalid() throws Exception {

                when(authService.validateToken("invalid-token"))
                                .thenReturn(false);

                mockMvc.perform(get("/auth/validate")
                                .header("Authorization", "Bearer invalid-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("Token inválido o expirado"));
        }

        @Test
        void testHealthCheck() throws Exception {

                mockMvc.perform(get("/auth/health"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").value("OK"))
                                .andExpect(jsonPath("$.message").value("Auth service running"));
        }
}