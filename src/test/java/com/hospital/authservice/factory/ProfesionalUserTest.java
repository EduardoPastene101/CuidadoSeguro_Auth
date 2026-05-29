package com.hospital.authservice.factory;

import com.hospital.authservice.dto.RegisterRequest;
import com.hospital.authservice.entity.Persona;
import com.hospital.authservice.entity.Profesional;
import com.hospital.authservice.entity.Role;
import com.hospital.authservice.entity.Usuario;
import com.hospital.authservice.repository.RoleRepository;
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
class ProfesionalUserTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private ProfesionalUser profesionalUser;

    private RegisterRequest registerRequest;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .nombre("ROLE_PROFESIONAL")
                .activo(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("doctor")
                .password("Password123!")
                .email("doctor@test.com")
                .tipoUsuario("PROFESIONAL")
                .nombres("Carlos")
                .apellidos("Gomez")
                .tipoDocumento("DNI")
                .numeroDocumento("87654321")
                .roles(List.of("ROLE_PROFESIONAL"))
                .numeroLicencia("LIC-12345")
                .especialidad("Cardiología")
                .universidad("Universidad de Chile")
                .anioGraduacion(2010)
                .build();
    }

    @Test
    void testCrearUsuarioSuccess() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = profesionalUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertEquals("doctor", usuario.getUsername());
        assertEquals("Password123!", usuario.getPassword());
        assertEquals("doctor@test.com", usuario.getEmail());
        assertTrue(usuario.getActivo());
        assertTrue(usuario.getNoBloqueado());
        assertNotNull(usuario.getPersona());
        assertNotNull(usuario.getProfesional());
        assertNotNull(usuario.getRoles());
        assertEquals(1, usuario.getRoles().size());
        verify(roleRepository).findByNombre("ROLE_PROFESIONAL");
    }

    @Test
    void testCrearUsuarioWithFullProfessionalInfo() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.of(role));
        registerRequest.setSubespecialidad("Cardiología Intervencionista");
        registerRequest.setExperienciaAnios(15);
        registerRequest.setInstitucion("Hospital Clinico");
        registerRequest.setHorasSemanales("40");

        // When
        Usuario usuario = profesionalUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertNotNull(usuario.getProfesional());
        assertEquals("LIC-12345", usuario.getProfesional().getNumeroLicencia());
        assertEquals("Cardiología", usuario.getProfesional().getEspecialidad());
        assertEquals("Cardiología Intervencionista", usuario.getProfesional().getSubespecialidad());
        assertEquals("Universidad de Chile", usuario.getProfesional().getUniversidad());
        assertEquals(2010, usuario.getProfesional().getAnioGraduacion());
        assertEquals(15, usuario.getProfesional().getExperienciaAnios());
        assertEquals("Hospital Clinico", usuario.getProfesional().getInstitucion());
        assertEquals(40, usuario.getProfesional().getHorasSemanales());
    }

    @Test
    void testCrearUsuarioWithDefaultExperience() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.of(role));
        registerRequest.setExperienciaAnios(null);

        // When
        Usuario usuario = profesionalUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertNotNull(usuario.getProfesional());
        assertEquals(0, usuario.getProfesional().getExperienciaAnios());
    }

    @Test
    void testCrearUsuarioRoleNotFound() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> profesionalUser.crearUsuario(registerRequest));
        verify(roleRepository).findByNombre("ROLE_PROFESIONAL");
    }

    @Test
    void testValidarDatosEspecificosSuccess() {
        // Given
        registerRequest.setRoles(List.of("ROLE_PROFESIONAL"));

        // When & Then
        assertDoesNotThrow(() -> profesionalUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testValidarDatosEspecificosMissingNumeroLicencia() {
        // Given
        registerRequest.setNumeroLicencia(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El número de licencia es obligatorio para médicos", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosEmptyNumeroLicencia() {
        // Given
        registerRequest.setNumeroLicencia("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El número de licencia es obligatorio para médicos", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosMissingEspecialidad() {
        // Given
        registerRequest.setEspecialidad(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("La especialidad es obligatoria para médicos", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosEmptyEspecialidad() {
        // Given
        registerRequest.setEspecialidad("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("La especialidad es obligatoria para médicos", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosMissingUniversidad() {
        // Given
        registerRequest.setUniversidad(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("La universidad de graduación es obligatoria para médicos", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosEmptyUniversidad() {
        // Given
        registerRequest.setUniversidad("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("La universidad de graduación es obligatoria para médicos", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosInvalidAnioGraduacionTooOld() {
        // Given
        registerRequest.setAnioGraduacion(1949);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El año de graduación no es válido", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosInvalidAnioGraduacionTooNew() {
        // Given
        registerRequest.setAnioGraduacion(2027);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El año de graduación no es válido", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosValidAnioGraduacion() {
        // Given
        registerRequest.setAnioGraduacion(2020);

        // When & Then
        assertDoesNotThrow(() -> profesionalUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testValidarDatosEspecificosMissingProfesionalRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER"));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profesionalUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El usuario médico debe tener el rol ROLE_PROFESIONAL", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosWithProfesionalRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER", "ROLE_PROFESIONAL"));

        // When & Then
        assertDoesNotThrow(() -> profesionalUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testGetTipoUsuario() {
        // When
        String tipoUsuario = profesionalUser.getTipoUsuario();

        // Then
        assertEquals("PROFESIONAL", tipoUsuario);
    }

    @Test
    void testCrearUsuarioPersonaFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.of(role));
        registerRequest.setFechaNacimiento(LocalDate.of(1985, 5, 15));
        registerRequest.setGenero("M");
        registerRequest.setTelefono("987654321");
        registerRequest.setDireccion("Calle Test 123");

        // When
        Usuario usuario = profesionalUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getPersona());
        assertEquals("Carlos", usuario.getPersona().getNombres());
        assertEquals("Gomez", usuario.getPersona().getApellidos());
        assertEquals("DNI", usuario.getPersona().getTipoDocumento());
        assertEquals("87654321", usuario.getPersona().getNumeroDocumento());
        assertEquals("1985-05-15", usuario.getPersona().getFechaNacimiento());
        assertEquals("M", usuario.getPersona().getGenero());
        assertEquals("987654321", usuario.getPersona().getTelefono());
        assertEquals("Calle Test 123", usuario.getPersona().getDireccion());
        assertTrue(usuario.getPersona().getActivo());
    }

    @Test
    void testCrearUsuarioProfesionalFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = profesionalUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getProfesional());
        assertEquals("LIC-12345", usuario.getProfesional().getNumeroLicencia());
        assertEquals("Cardiología", usuario.getProfesional().getEspecialidad());
        assertEquals("Universidad de Chile", usuario.getProfesional().getUniversidad());
        assertEquals(2010, usuario.getProfesional().getAnioGraduacion());
        assertTrue(usuario.getProfesional().getActivo());
        assertNotNull(usuario.getProfesional().getFechaCreacion());
    }

    @Test
    void testCrearUsuarioRelationships() {
        // Given
        when(roleRepository.findByNombre("ROLE_PROFESIONAL")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = profesionalUser.crearUsuario(registerRequest);

        // Then
        assertEquals(usuario, usuario.getPersona().getUsuario());
        assertEquals(usuario, usuario.getProfesional().getUsuario());
    }
}
