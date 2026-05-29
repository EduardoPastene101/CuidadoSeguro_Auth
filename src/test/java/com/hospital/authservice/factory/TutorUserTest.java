package com.hospital.authservice.factory;

import com.hospital.authservice.dto.RegisterRequest;
import com.hospital.authservice.entity.Persona;
import com.hospital.authservice.entity.Role;
import com.hospital.authservice.entity.Tutor;
import com.hospital.authservice.entity.Usuario;
import com.hospital.authservice.repository.PacienteRepository;
import com.hospital.authservice.repository.RoleRepository;
import com.hospital.authservice.repository.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorUserTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private TutorUser tutorUser;

    private RegisterRequest registerRequest;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .nombre("ROLE_TUTOR")
                .activo(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("tutor")
                .password("Password123!")
                .email("tutor@test.com")
                .tipoUsuario("TUTOR")
                .nombres("Maria")
                .apellidos("Rodriguez")
                .tipoDocumento("DNI")
                .numeroDocumento("11111111")
                .roles(List.of("ROLE_TUTOR"))
                .pacientesRuts(List.of("12345678", "87654321"))
                .build();
    }

    @Test
    void testCrearUsuarioSuccess() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = tutorUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertEquals("tutor", usuario.getUsername());
        assertEquals("Password123!", usuario.getPassword());
        assertEquals("tutor@test.com", usuario.getEmail());
        assertTrue(usuario.getActivo());
        assertTrue(usuario.getNoBloqueado());
        assertNotNull(usuario.getPersona());
        assertNotNull(usuario.getTutor());
        assertNotNull(usuario.getRoles());
        assertEquals(1, usuario.getRoles().size());
        verify(roleRepository).findByNombre("ROLE_TUTOR");
    }

    @Test
    void testCrearUsuarioWithPacientesRuts() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));
        registerRequest.setPacientesRuts(List.of("12345678", "87654321", "11112222"));

        // When
        Usuario usuario = tutorUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertNotNull(usuario.getTutor());
        assertEquals(3, usuario.getTutor().getPacientesRuts().size());
        assertTrue(usuario.getTutor().getPacientesRuts().contains("12345678"));
        assertTrue(usuario.getTutor().getPacientesRuts().contains("87654321"));
        assertTrue(usuario.getTutor().getPacientesRuts().contains("11112222"));
    }

    @Test
    void testCrearUsuarioRoleNotFound() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> tutorUser.crearUsuario(registerRequest));
        verify(roleRepository).findByNombre("ROLE_TUTOR");
    }

    @Test
    void testCrearUsuarioEmptyPacientesRuts() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));
        registerRequest.setPacientesRuts(List.of());

        // When & Then
        assertThrows(RuntimeException.class, () -> tutorUser.crearUsuario(registerRequest));
    }

    @Test
    void testValidarDatosEspecificosSuccess() {
        // Given
        registerRequest.setRoles(List.of("ROLE_TUTOR"));

        // When & Then
        assertDoesNotThrow(() -> tutorUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testValidarDatosEspecificosNullPacientesRuts() {
        // Given
        registerRequest.setPacientesRuts(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tutorUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("Debe asociar al menos un paciente", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosEmptyPacientesRuts() {
        // Given
        registerRequest.setPacientesRuts(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tutorUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("Debe asociar al menos un paciente", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosMissingTutorRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER"));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tutorUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("Debe incluir ROLE_TUTOR", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosWithTutorRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER", "ROLE_TUTOR"));

        // When & Then
        assertDoesNotThrow(() -> tutorUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testGetTipoUsuario() {
        // When
        String tipoUsuario = tutorUser.getTipoUsuario();

        // Then
        assertEquals("TUTOR", tipoUsuario);
    }

    @Test
    void testCrearUsuarioPersonaFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));
        registerRequest.setFechaNacimiento(LocalDate.of(1975, 3, 20));
        registerRequest.setGenero("F");
        registerRequest.setTelefono("555555555");
        registerRequest.setDireccion("Avenida Tutor 456");

        // When
        Usuario usuario = tutorUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getPersona());
        assertEquals("Maria", usuario.getPersona().getNombres());
        assertEquals("Rodriguez", usuario.getPersona().getApellidos());
        assertEquals("DNI", usuario.getPersona().getTipoDocumento());
        assertEquals("11111111", usuario.getPersona().getNumeroDocumento());
        assertEquals("1975-03-20", usuario.getPersona().getFechaNacimiento());
        assertEquals("F", usuario.getPersona().getGenero());
        assertEquals("555555555", usuario.getPersona().getTelefono());
        assertEquals("Avenida Tutor 456", usuario.getPersona().getDireccion());
        assertTrue(usuario.getPersona().getActivo());
    }

    @Test
    void testCrearUsuarioTutorFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = tutorUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getTutor());
        assertEquals(2, usuario.getTutor().getPacientesRuts().size());
        assertTrue(usuario.getTutor().getPacientesRuts().contains("12345678"));
        assertTrue(usuario.getTutor().getPacientesRuts().contains("87654321"));
    }

    @Test
    void testCrearUsuarioRelationships() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = tutorUser.crearUsuario(registerRequest);

        // Then
        assertEquals(usuario, usuario.getPersona().getUsuario());
        assertEquals(usuario, usuario.getTutor().getUsuario());
    }

    @Test
    void testCrearUsuarioSinglePacienteRut() {
        // Given
        when(roleRepository.findByNombre("ROLE_TUTOR")).thenReturn(Optional.of(role));
        registerRequest.setPacientesRuts(List.of("12345678"));

        // When
        Usuario usuario = tutorUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertNotNull(usuario.getTutor());
        assertEquals(1, usuario.getTutor().getPacientesRuts().size());
        assertEquals("12345678", usuario.getTutor().getPacientesRuts().get(0));
    }
}
