package com.hospital.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.authservice.dto.*;
import com.hospital.authservice.service.AuthService;

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

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

        private MockMvc mockMvc;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                this.mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
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

                when(authService.login(any(LoginRequest.class))).thenReturn(response);

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

                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new RuntimeException("Credenciales inválidas"));

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

                when(authService.register(any(RegisterRequest.class))).thenReturn(response);

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
                // Given
                RegisterRequest request = RegisterRequest.builder()
                                .username("") // Invalid username
                                .password("Password123!")
                                .email("invalid-email") // Invalid email
                                .tipoUsuario("PACIENTE")
                                .build();

                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new RuntimeException("Datos inválidos"));

                // When & Then
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

                when(authService.refreshToken(any(RefreshRequest.class))).thenReturn(response);

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

                when(authService.refreshToken(any(RefreshRequest.class)))
                                .thenThrow(new RuntimeException("Refresh token inválido"));

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
                when(authService.logout(any(LogoutRequest.class))).thenReturn(response);

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
                // Given
                when(authService.logout(any(LogoutRequest.class))).thenThrow(new RuntimeException("Error en logout"));

                // When & Then
                mockMvc.perform(post("/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testValidateTokenSuccess() throws Exception {
                // When & Then
                mockMvc.perform(get("/auth/validate")
                                .param("token", "valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").value(true))
                                .andExpect(jsonPath("$.message").value("Token válido"));
        }

        @Test
        void testValidateTokenInvalid() throws Exception {
                // When & Then
                mockMvc.perform(get("/auth/validate")
                                .param("token", "invalid-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
        }

        @Test
        void testHealthCheck() throws Exception {
                // When & Then
                mockMvc.perform(get("/auth/health"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").value("Auth service is running"))
                                .andExpect(jsonPath("$.errorCode").value("OK"));
        }
}
