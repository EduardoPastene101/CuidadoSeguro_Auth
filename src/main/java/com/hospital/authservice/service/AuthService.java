package com.hospital.authservice.service;

import com.hospital.authservice.dto.*;
import com.hospital.authservice.entity.*;
import com.hospital.authservice.exception.AuthException;
import com.hospital.authservice.exception.DownstreamServiceException;
import com.hospital.authservice.factory.UserFactory;
import com.hospital.authservice.repository.*;
import com.hospital.authservice.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Slf4j
@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RoleRepository roleRepository;
    private final PersonaRepository personaRepository;
    private final ProfesionalRepository profesionalRepository;
    private final PacienteRepository pacienteRepository;
    private final TutorRepository tutorRepository;
    private final UserFactory userFactory;
    private final ConsentimientoRepository consentimientoRepository;
    private final TerminosCondicionesRepository terminosCondicionesRepository;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public Paciente obtenerDetallesUsuario(String token) {

        try {
            token = token.replace("Bearer ", "").trim();

            if (token.isEmpty()) {
                throw new AuthException("Token no proporcionado");
            }

            if (!validateToken(token)) {
                throw new AuthException("Token inválido o revocado");
            }

            // 1. Extraer userId desde JWT
            Long userId = jwtService.getUserIdFromToken(token);

            if (userId == null) {
                throw new AuthException("Token sin userId");
            }

            log.info("UserId desde JWT: {}", userId);

            // 2. Buscar usuario por ID
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Usuario no encontrado en BD. ID usado: {}", userId);
                        return new AuthException("Usuario no encontrado");
                    });

            return usuario.getPaciente();

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error obteniendo detalles de usuario", e);
            throw new AuthException("Error procesando token");
        }
    }

    @Transactional
    public boolean validateToken(String token) {
        try {
            // Verifica si el token está en blacklist
            boolean isBlacklisted = tokenBlacklistRepository.existsByToken(token);
            if (isBlacklisted) {
                return false;
            }

            // Validar token con JWT
            return jwtService.isTokenValid(token);

        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login para usuario: {}", request.getUsername());

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(userDetails.getUsername());

            if (usuarioOpt.isEmpty()) {
                throw new AuthException("Usuario no encontrado");
            }

            Usuario usuario = usuarioOpt.get();

            // generar JWT
            // Actualizar último login
            usuarioRepository.actualizarUltimoLogin(usuario.getId(), LocalDateTime.now());

            // Generar tokens
            Map<String, Object> accessClaims = new HashMap<>();
            Map<String, Object> refreshClaims = new HashMap<>();

            String accessToken = jwtService.generateToken(
                    accessClaims,
                    usuario);

            String refreshToken = jwtService.generateRefreshToken(
                    refreshClaims,
                    usuario,
                    usuario.getEmail());

            // Guardar refresh token en BD
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .token(refreshToken)
                    .usuario(usuario)
                    .fechaExpiracion(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration()))
                    .build();

            refreshTokenRepository.save(refreshTokenEntity);

            // Revocar tokens anteriores
            refreshTokenRepository.revocarTodosTokensUsuario(usuario);

            String username = usuario.getUsername();
            String email = usuario.getEmail();

            // Construir response
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .id(usuario.getId())
                    .username(username)
                    .email(email)
                    .nombreCompleto(
                            usuario.getPersona().getNombres()
                                    + " "
                                    + usuario.getPersona().getApellidos())
                    .telefono(usuario.getPersona().getTelefono())
                    .direccion(usuario.getPersona().getDireccion())
                    .genero(usuario.getPersona().getGenero())
                    .tipoUsuario(determinarTipoUsuario(usuario))
                    .profesion(usuario.getProfesional() != null ? usuario.getProfesional().getProfesion() : null)

                    .pacientesRuts(
                            usuario.getTutor() != null
                                    ? usuario.getTutor().getPacientesRuts()
                                    : null)

                    .roles(
                            usuario.getAuthorities().stream()
                                    .map(auth -> auth.getAuthority())
                                    .toList())

                    .build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .userInfo(userInfo)
                    .message("Login exitoso")
                    .build();

            log.info("Login exitoso para usuario: {}", request.getUsername());
            return response;

        } catch (BadCredentialsException e) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(request.getUsername());
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                int intentos = usuario.getIntentosFallidos() + 1;

                usuarioRepository.actualizarIntentosFallidos(usuario.getId(), intentos);

                if (intentos >= 5) {
                    usuarioRepository.bloquearUsuario(usuario.getId());
                    log.warn("Usuario bloqueado por exceder intentos fallidos: {}", request.getUsername());
                }
            }

            log.warn("Credenciales inválidas para usuario: {}", request.getUsername());
            throw new AuthException("Credenciales inválidas");
        }
    }

    public static boolean tieneCaracterEspecial(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        // Coincide si hay al menos un carácter que no sea letra ni número
        return !s.matches("[A-Za-z0-9 ]*"); // El espacio está permitido aquí
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intento de registro para usuario: {}", request.getUsername());

        // Validaciones básicas
        // Validaciones básicas
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("El username ya está en uso");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("El email ya está en uso");
        }

        if (personaRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new AuthException("El número de documento ya está registrado");
        }

        // Validar tipo de usuario
        if (!userFactory.isSupportedUserType(request.getTipoUsuario())) {
            throw new AuthException("Tipo de usuario no soportado: " + request.getTipoUsuario());
        }

        // Validar términos y condiciones
        if (request.getAceptaTerminos() == null || !request.getAceptaTerminos()) {
            throw new AuthException("Debe aceptar los términos y condiciones");
        }

        // Crear usuario usando factory (username ya está encriptado)
        com.hospital.authservice.factory.User userCreator = userFactory.createUser(request.getTipoUsuario());
        Usuario usuario = userCreator.crearUsuario(request);
        usuario.setEmail(request.getEmail());
        if (request.getPassword().chars().anyMatch(Character::isLowerCase) &&
                request.getPassword().chars().anyMatch(Character::isUpperCase) &&
                request.getPassword().chars().anyMatch(Character::isDigit) &&
                tieneCaracterEspecial(request.getPassword())) {

            // Encriptar contraseña
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        } else {
            String ret = "La contraseña debe tener al menos: ";

            if (!request.getPassword().chars().anyMatch(Character::isLowerCase)) {
                ret = ret + "1 letra minúscula";
            }

            if (!request.getPassword().chars().anyMatch(Character::isUpperCase)) {
                if (ret.contains("1")) {
                    ret = ret + ",";
                }
                ret = ret + "1 letra mayúscula";
            }

            if (!request.getPassword().chars().anyMatch(Character::isDigit)) {
                if (ret.contains("1")) {
                    ret = ret + ",";
                }
                ret = ret + "1 número";
            }

            if (!tieneCaracterEspecial(request.getPassword().toString())) {
                if (ret.contains("1")) {
                    ret = ret + ",";
                }
                ret = ret + "1 carácter especial";
            }

            ret = ret + ".";

            throw new AuthException(ret);
        }

        if (request.getPassword().toString().length() < 8) {
            throw new AuthException("La contraseña debe tener al menos 8 carácteres.");
        }

        if (request.getPassword().toString().length() > 100) {
            throw new AuthException("La contraseña debe tener menos de 101 carácteres, ahora tiene: "
                    + request.getPassword().toString() + " carácteres.");
        }

        System.out.println(
                "PACIENTES RUTS: "
                        + request.getPacientesRuts());

        // Validaciones específicas según tipo
        if ("PROFESIONAL".equals(request.getTipoUsuario())) {
            if (profesionalRepository.existsByNumeroLicencia(request.getNumeroLicencia())) {
                throw new AuthException("El número de licencia ya está registrada");
            }
        }

        if ("PACIENTE".equals(request.getTipoUsuario())
                && request.getHistoriaClinica() != null
                && pacienteRepository.existsByHistoriaClinica(request.getHistoriaClinica())) {

            throw new AuthException("La historia clínica ya está registrada");
        }

        // Guardar entidades
        usuarioRepository.save(usuario);

        // Crear paciente en microservicio pacientes
        // Crear paciente en microservicio pacientes
        if ("PACIENTE".equals(request.getTipoUsuario())) {

            PacienteMicroDto pacienteDto = new PacienteMicroDto();

            pacienteDto.setRut(request.getNumeroDocumento());
            pacienteDto.setNombre(request.getNombres());
            pacienteDto.setApellido(request.getApellidos());
            pacienteDto.setEmail(request.getEmail());
            pacienteDto.setAlergias(request.getAlergias());
            pacienteDto.setDireccion(request.getDireccion());
            pacienteDto.setTelefono(request.getTelefono());
            pacienteDto.setGenero(request.getGenero());
            pacienteDto.setFechaNacimiento(request.getFechaNacimiento());

            // Generar token JWT para enviar al microservicio
            Map<String, Object> claims = new HashMap<>();

            String accessToken = jwtService.generateToken(
                    claims,
                    usuario);

            // Headers con token
            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(accessToken);

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setAccept(List.of(
                    MediaType.APPLICATION_JSON
            ));
            HttpEntity<PacienteMicroDto> entity = new HttpEntity<>(pacienteDto, headers);

            log.info("ANTES DE PACIENTES");

            try {
                restTemplate.exchange(
                        "http://paciente-ms-493696077.us-east-1.elb.amazonaws.com:8082/pacientes",
                        HttpMethod.POST,
                        entity,
                        Object.class);
            } catch (Exception e) {
                log.error("Fallo al crear paciente en paciente-ms durante registro de '{}': {}",
                        request.getUsername(), e.getMessage(), e);
                throw new DownstreamServiceException(
                        "paciente-ms",
                        "No se pudo crear el registro de paciente. Intente nuevamente en unos minutos.",
                        e);
            }

            log.info("DESPUÉS DE PACIENTES");
            FichaClinicaMicroDto fichaDto = new FichaClinicaMicroDto();

            fichaDto.setNombrePaciente(
                    request.getNombres() + " "
                            + request.getApellidos());

            fichaDto.setRutPaciente(
                    request.getNumeroDocumento());

            fichaDto.setEdad(
                    calcularEdad(
                            request.getFechaNacimiento()));

            fichaDto.setDiagnostico(
                    request.getEnfermedadesCronicas());

            fichaDto.setAlergias(
                    request.getAlergias());

            fichaDto.setObservaciones("");

            fichaDto.setGenero(
                    request.getGenero());

            HttpEntity<FichaClinicaMicroDto> fichaEntity = new HttpEntity<>(fichaDto, headers);

            try {
                restTemplate.exchange(
                        "http://dm-ms-401060910.us-east-1.elb.amazonaws.com:8083/fichas",
                        HttpMethod.POST,
                        fichaEntity,
                        Object.class);
            } catch (Exception e) {
                log.error("Fallo al crear ficha clínica en dm-ms durante registro de '{}': {}",
                        request.getUsername(), e.getMessage(), e);
                throw new DownstreamServiceException(
                        "dm-ms",
                        "No se pudo crear la ficha clínica. Intente nuevamente en unos minutos.",
                        e);
            }
        }

        // Guardar consentimiento de términos y condiciones
        if (request.getVersionTerminos() != null) {
            Optional<TerminosCondiciones> terminosOpt = terminosCondicionesRepository
                    .findByVersion(request.getVersionTerminos());
            if (terminosOpt.isPresent()) {
                Consentimiento consentimiento = Consentimiento.builder()
                        .usuario(usuario)
                        .terminosCondiciones(terminosOpt.get())
                        .aceptado(true)
                        .build();
                consentimientoRepository.save(consentimiento);
            }
        }

        // Generar tokens para login automático
        /*
         * //Forma antigua y estable:
         * String accessToken = jwtService.generateToken(usuario);
         * String refreshToken = jwtService.generateRefreshToken(usuario);
         */

        Map<String, Object> accessClaims = new HashMap<>();
        Map<String, Object> refreshClaims = new HashMap<>();

        String accessToken = jwtService.generateToken(
                accessClaims,
                usuario);

        String refreshToken = jwtService.generateRefreshToken(
                refreshClaims,
                usuario,
                usuario.getEmail());
        // Guardar refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // Construir response
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .nombreCompleto(usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos())
                .telefono(usuario.getPersona().getTelefono())
                .direccion(usuario.getPersona().getDireccion())
                .genero(usuario.getPersona().getGenero())
                .tipoUsuario(request.getTipoUsuario())
                .profesion(usuario.getProfesional() != null ? usuario.getProfesional().getProfesion() : null)
                .pacientesRuts(
                        usuario.getTutor() != null
                                ? usuario.getTutor().getPacientesRuts()
                                : null)
                .roles(usuario.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .toList())
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .userInfo(userInfo)
                .message("Registro exitoso")
                .build();

        log.info("Registro exitoso para usuario: {}", request.getUsername());
        return response;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        log.debug("Intento de refresh token");

        String refreshToken = request.getRefreshToken();

        // Validar refresh token
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (refreshTokenOpt.isEmpty()) {
            throw new AuthException("Refresh token no encontrado");
        }

        RefreshToken refreshTokenEntity = refreshTokenOpt.get();

        // Validar que no esté revocado ni usado ni expirado
        if (refreshTokenEntity.getRevocado() || refreshTokenEntity.getUsado() || refreshTokenEntity.isExpirado()) {
            throw new AuthException("Refresh token inválido");
        }

        // Obtener usuario
        Usuario usuario = refreshTokenEntity.getUsuario();

        // Marcar refresh token actual como usado
        refreshTokenEntity.marcarComoUsado();
        refreshTokenRepository.save(refreshTokenEntity);

        // Generar nuevos tokens

        Map<String, Object> accessClaims = new HashMap<>();
        Map<String, Object> refreshClaims = new HashMap<>();

        String newAccessToken = jwtService.generateToken(
                accessClaims,
                usuario);

        String newRefreshToken = jwtService.generateRefreshToken(
                refreshClaims,
                usuario,
                usuario.getEmail());

        // Guardar nuevo refresh token
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration()))
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        // Construir response
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .nombreCompleto(usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos())
                .telefono(usuario.getPersona().getTelefono())
                .direccion(usuario.getPersona().getDireccion())
                .genero(usuario.getPersona().getGenero())
                .tipoUsuario(determinarTipoUsuario(usuario))
                .pacientesRuts(
                        usuario.getTutor() != null
                                ? usuario.getTutor().getPacientesRuts()
                                : null)
                .roles(usuario.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .toList())
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .userInfo(userInfo)
                .message("Token refrescado exitosamente")
                .build();

        log.debug("Refresh token exitoso para usuario: {}", usuario.getUsername());
        return response;
    }

    @Transactional
    public ApiResponseDto<Void> logout(LogoutRequest request) {
        log.info("Intento de logout");

        String accessToken = request.getAccessToken();
        String refreshToken = request.getRefreshToken();

        // Agregar access token a blacklist
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                LocalDateTime expiracion = jwtService.getExpirationDate(accessToken);

                TokenBlacklist blacklistToken = TokenBlacklist.builder()
                        .token(accessToken)
                        .fechaExpiracion(expiracion)
                        .motivo("Logout")
                        .build();

                tokenBlacklistRepository.save(blacklistToken);
                log.debug("Access token agregado a blacklist");

            } catch (Exception e) {
                log.warn("No se pudo procesar el access token para blacklist: {}", e.getMessage());
            }
        }

        // Revocar refresh token
        if (refreshToken != null && !refreshToken.isEmpty()) {
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(refreshToken);
            if (refreshTokenOpt.isPresent()) {
                RefreshToken refreshTokenEntity = refreshTokenOpt.get();
                refreshTokenEntity.revocar();
                refreshTokenRepository.save(refreshTokenEntity);
                log.debug("Refresh token revocado");
            }
        }

        log.info("Logout exitoso");
        return ApiResponseDto.success(null, "Logout exitoso");
    }

    @Transactional
    public void limpiarTokensExpirados() {
        log.debug("Limpiando tokens expirados");

        LocalDateTime ahora = LocalDateTime.now();

        // Limpiar refresh tokens expirados
        refreshTokenRepository.eliminarTokensExpirados(ahora);

        // Limpiar tokens de blacklist expirados
        tokenBlacklistRepository.eliminarTokensExpirados(ahora);

        log.debug("Limpieza de tokens expirados completada");
    }

    private String determinarTipoUsuario(Usuario usuario) {
        if (usuario.getProfesional() != null) {
            return "PROFESIONAL";
        } else if (usuario.getPaciente() != null) {
            return "PACIENTE";
        } else if (usuario.getTutor() != null) {
            return "TUTOR";
        } else {
            return "ADMIN";
        }
    }

    private Integer calcularEdad(LocalDate fechaNacimiento) {

        if (fechaNacimiento == null) {
            return 0;
        }

        return Period
                .between(fechaNacimiento, LocalDate.now())
                .getYears();
    }
}