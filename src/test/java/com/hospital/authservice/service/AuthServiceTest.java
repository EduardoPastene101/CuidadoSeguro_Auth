package com.hospital.authservice.service;

import com.hospital.authservice.dto.*;
import com.hospital.authservice.entity.*;
import com.hospital.authservice.exception.AuthException;
import com.hospital.authservice.factory.UserFactory;
import com.hospital.authservice.repository.*;
import com.hospital.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private UserFactory userFactory;

    @Mock
    private ConsentimientoRepository consentimientoRepository;

    @Mock
    private TerminosCondicionesRepository terminosCondicionesRepository;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private RefreshRequest refreshRequest;
    private LogoutRequest logoutRequest;
    private Usuario usuario;
    private Role role;
    private Persona persona;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .password("Password123!")
                .email("newuser@test.com")
                .tipoUsuario("PACIENTE")
                .nombres("Juan")
                .apellidos("Perez")
                .tipoDocumento("DNI")
                .numeroDocumento("12345678")
                .historiaClinica("HC-001")
                .aceptaTerminos(true)
                .versionTerminos(1)
                .roles(List.of("ROLE_PACIENTE"))
                .build();

        refreshRequest = RefreshRequest.builder()
                .refreshToken("refresh-token")
                .build();

        logoutRequest = LogoutRequest.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        role = Role.builder()
                .id(1L)
                .nombre("ROLE_PACIENTE")
                .activo(true)
                .build();

        persona = Persona.builder()
                .id(1L)
                .nombres("Juan")
                .apellidos("Perez")
                .tipoDocumento("DNI")
                .numeroDocumento("12345678")
                .activo(true)
                .build();

        usuario = Usuario.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .email("testuser@test.com")
                .activo(true)
                .noBloqueado(true)
                .persona(persona)
                .roles(Set.of(role))
                .build();
    }

    @Test
    void testLoginSuccess() {
        // Given
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(usuario);

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtService.generateToken(anyMap(), any(UserDetails.class)))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(
                anyMap(),
                any(UserDetails.class),
                anyString()))
                .thenReturn("refresh-token");

        when(jwtService.getJwtExpiration()).thenReturn(900L);

        when(jwtService.getRefreshExpiration()).thenReturn(604800L);

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertEquals("Login exitoso", response.getMessage());

        verify(usuarioRepository)
                .actualizarUltimoLogin(eq(1L), any(LocalDateTime.class));

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));

        verify(refreshTokenRepository)
                .revocarTodosTokensUsuario(usuario);
    }

    @Test
    void testLoginFailure() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));
        when(usuarioRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(usuario));

        // When & Then
        assertThrows(AuthException.class, () -> authService.login(loginRequest));

        verify(usuarioRepository).actualizarIntentosFallidos(eq(1L), eq(1));
    }

    @Test
    void testRegisterSuccess() {
        // Given
        com.hospital.authservice.factory.User userCreator = mock(com.hospital.authservice.factory.User.class);

        when(userFactory.isSupportedUserType("PACIENTE"))
                .thenReturn(true);

        when(userFactory.createUser("PACIENTE"))
                .thenReturn(userCreator);

        when(userCreator.crearUsuario(registerRequest))
                .thenReturn(usuario);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuario);

        when(jwtService.generateToken(anyMap(), any(UserDetails.class)))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(
                anyMap(),
                any(UserDetails.class),
                anyString()))
                .thenReturn("refresh-token");

        when(jwtService.getJwtExpiration()).thenReturn(900L);

        when(jwtService.getRefreshExpiration()).thenReturn(604800L);

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Registro exitoso", response.getMessage());

        verify(usuarioRepository).save(any(Usuario.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testRegisterUsernameAlreadyExists() {
        // Given
        when(usuarioRepository.existsByUsername(anyString()))
                .thenReturn(true);

        // When & Then
        assertThrows(AuthException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testRegisterEmailAlreadyExists() {
        // Given
        when(usuarioRepository.existsByUsername(anyString()))
                .thenReturn(false);

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(true);
        // When & Then
        assertThrows(AuthException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testRegisterUnsupportedUserType() {
        // Given
        registerRequest.setTipoUsuario("UNSUPPORTED");
        when(userFactory.isSupportedUserType("UNSUPPORTED")).thenReturn(false);

        // When & Then
        assertThrows(AuthException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testRefreshTokenSuccess() {
        // Given
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .revocado(false)
                .usado(false)
                .build();

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        when(jwtService.generateToken(
                anyMap(),
                any(UserDetails.class)))
                .thenReturn("new-access-token");

        when(jwtService.generateRefreshToken(
                anyMap(),
                any(UserDetails.class),
                anyString()))
                .thenReturn("new-refresh-token");

        // When
        AuthResponse response = authService.refreshToken(refreshRequest);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("Token refrescado exitosamente", response.getMessage());

        assertTrue(refreshToken.getUsado());

        verify(refreshTokenRepository, times(2))
                .save(any(RefreshToken.class));
    }

    @Test
    void testRefreshTokenNotFound() {
        // Given
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthException.class, () -> authService.refreshToken(refreshRequest));
    }

    @Test
    void testRefreshTokenExpired() {
        // Given
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().minusDays(1))
                .revocado(false)
                .usado(false)
                .build();

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        // When & Then
        assertThrows(AuthException.class, () -> authService.refreshToken(refreshRequest));
    }

    @Test
    void testLogoutSuccess() {
        // Given
        when(jwtService.getExpirationDate("access-token")).thenReturn(LocalDateTime.now().plusMinutes(10));
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .revocado(false)
                .usado(false)
                .build();
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        // When
        ApiResponseDto<Void> response = authService.logout(logoutRequest);

        // Then
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Logout exitoso", response.getMessage());

        verify(tokenBlacklistRepository).save(any(TokenBlacklist.class));
        assertTrue(refreshToken.getRevocado());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void testLimpiarTokensExpirados() {
        // Given
        LocalDateTime ahora = LocalDateTime.now();

        // When
        authService.limpiarTokensExpirados();

        // Then
        verify(refreshTokenRepository)
                .eliminarTokensExpirados(any(LocalDateTime.class));

        verify(tokenBlacklistRepository)
                .eliminarTokensExpirados(any(LocalDateTime.class));
    }

    @Test
    void testObtenerDetallesUsuarioSuccess() {
        // Given
        String token = "Bearer valid-token";
        Paciente paciente = Paciente.builder()
                .id(1L)
                .historiaClinica("HC-001")
                .build();
        usuario.setPaciente(paciente);

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // When
        Paciente result = authService.obtenerDetallesUsuario(token);

        // Then
        assertNotNull(result);
        assertEquals(paciente, result);
    }

    @Test
    void testObtenerDetallesUsuarioTokenNotProvided() {
        // Given
        String token = "";

        // When & Then
        assertThrows(AuthException.class, () -> authService.obtenerDetallesUsuario(token));
    }

    @Test
    void testObtenerDetallesUsuarioInvalidToken() {
        // Given
        String token = "Bearer invalid-token";
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        // When & Then
        assertThrows(AuthException.class, () -> authService.obtenerDetallesUsuario(token));
    }

    @Test
    void testObtenerDetallesUsuarioTokenBlacklisted() {
        // Given
        String token = "Bearer blacklisted-token";
        when(tokenBlacklistRepository.existsByToken("blacklisted-token")).thenReturn(true);

        // When & Then
        assertThrows(AuthException.class, () -> authService.obtenerDetallesUsuario(token));
    }

    @Test
    void testObtenerDetallesUsuarioUserIdNull() {
        // Given
        String token = "Bearer valid-token";
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(null);

        // When & Then
        assertThrows(AuthException.class, () -> authService.obtenerDetallesUsuario(token));
    }

    @Test
    void testObtenerDetallesUsuarioUsuarioNotFound() {
        // Given
        String token = "Bearer valid-token";
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthException.class, () -> authService.obtenerDetallesUsuario(token));
    }

    @Test
    void testValidateTokenSuccess() {
        // Given
        String token = "valid-token";
        when(tokenBlacklistRepository.existsByToken(token)).thenReturn(false);
        when(jwtService.isTokenValid(token)).thenReturn(true);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateTokenBlacklisted() {
        // Given
        String token = "blacklisted-token";
        when(tokenBlacklistRepository.existsByToken(token)).thenReturn(true);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateTokenInvalid() {
        // Given
        String token = "invalid-token";
        when(tokenBlacklistRepository.existsByToken(token)).thenReturn(false);
        when(jwtService.isTokenValid(token)).thenReturn(false);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateTokenException() {
        // Given
        String token = "exception-token";
        when(tokenBlacklistRepository.existsByToken(token)).thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertFalse(result);
    }

    @Test
    void testTieneCaracterEspecialWithSpecialChar() {
        // When
        boolean result = AuthService.tieneCaracterEspecial("password!");

        // Then
        assertTrue(result);
    }

    @Test
    void testTieneCaracterEspecialWithoutSpecialChar() {
        // When
        boolean result = AuthService.tieneCaracterEspecial("password123");

        // Then
        assertFalse(result);
    }

    @Test
    void testTieneCaracterEspecialNull() {
        // When
        boolean result = AuthService.tieneCaracterEspecial(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testTieneCaracterEspecialEmpty() {
        // When
        boolean result = AuthService.tieneCaracterEspecial("");

        // Then
        assertFalse(result);
    }

    @Test
    void testTieneCaracterEspecialWithSpace() {
        // When
        boolean result = AuthService.tieneCaracterEspecial("password 123");

        // Then
        assertFalse(result);
    }

    @Test
    void testTieneCaracterEspecialWithMultipleSpecialChars() {
        // When
        boolean result = AuthService.tieneCaracterEspecial("p@ssw0rd!");

        // Then
        assertTrue(result);
    }
}
