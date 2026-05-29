package com.hospital.authservice.factory;

import com.hospital.authservice.dto.RegisterRequest;
import com.hospital.authservice.entity.Paciente;
import com.hospital.authservice.entity.Persona;
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
class PacienteUserTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private PacienteUser pacienteUser;

    private RegisterRequest registerRequest;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .nombre("ROLE_PACIENTE")
                .activo(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("paciente")
                .password("Password123!")
                .email("paciente@test.com")
                .tipoUsuario("PACIENTE")
                .nombres("Juan")
                .apellidos("Perez")
                .tipoDocumento("DNI")
                .numeroDocumento("12345678")
                .roles(List.of("ROLE_PACIENTE"))
                .grupoSanguineo("A+")
                .factorRh("+")
                .build();
    }

    @Test
    void testCrearUsuarioSuccess() {
        // Given
        when(roleRepository.findByNombre("ROLE_PACIENTE")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = pacienteUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertEquals("paciente", usuario.getUsername());
        assertEquals("Password123!", usuario.getPassword());
        assertEquals("paciente@test.com", usuario.getEmail());
        assertTrue(usuario.getActivo());
        assertTrue(usuario.getNoBloqueado());
        assertNotNull(usuario.getPersona());
        assertNotNull(usuario.getPaciente());
        assertNotNull(usuario.getRoles());
        assertEquals(1, usuario.getRoles().size());
        verify(roleRepository).findByNombre("ROLE_PACIENTE");
    }

    @Test
    void testCrearUsuarioWithMedicalInfo() {
        // Given
        when(roleRepository.findByNombre("ROLE_PACIENTE")).thenReturn(Optional.of(role));
        registerRequest.setAlergias("Penicilina");
        registerRequest.setEnfermedadesCronicas("Diabetes");
        registerRequest.setMedicamentosActuales("Insulina");
        registerRequest.setContactoEmergencia("Maria Perez");
        registerRequest.setTelefonoEmergencia("123456789");
        registerRequest.setPrevision("FONASA");

        // When
        Usuario usuario = pacienteUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertNotNull(usuario.getPaciente());
        assertEquals("Penicilina", usuario.getPaciente().getAlergias());
        assertEquals("Diabetes", usuario.getPaciente().getEnfermedadesCronicas());
        assertEquals("Insulina", usuario.getPaciente().getMedicamentosActuales());
        assertEquals("Maria Perez", usuario.getPaciente().getContactoEmergencia());
        assertEquals("123456789", usuario.getPaciente().getTelefonoEmergencia());
        assertEquals("FONASA", usuario.getPaciente().getPrevision());
    }

    @Test
    void testCrearUsuarioRoleNotFound() {
        // Given
        when(roleRepository.findByNombre("ROLE_PACIENTE")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> pacienteUser.crearUsuario(registerRequest));
        verify(roleRepository).findByNombre("ROLE_PACIENTE");
    }

    @Test
    void testValidarDatosEspecificosSuccess() {
        // Given
        registerRequest.setRoles(List.of("ROLE_PACIENTE"));

        // When & Then
        assertDoesNotThrow(() -> pacienteUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testValidarDatosEspecificosMissingPacienteRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER"));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> pacienteUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El usuario paciente debe tener el rol ROLE_PACIENTE", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosWithPacienteRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER", "ROLE_PACIENTE"));

        // When & Then
        assertDoesNotThrow(() -> pacienteUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testGetTipoUsuario() {
        // When
        String tipoUsuario = pacienteUser.getTipoUsuario();

        // Then
        assertEquals("PACIENTE", tipoUsuario);
    }

    @Test
    void testCrearUsuarioPersonaFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_PACIENTE")).thenReturn(Optional.of(role));
        registerRequest.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        registerRequest.setGenero("M");
        registerRequest.setTelefono("123456789");
        registerRequest.setDireccion("Test Address");

        // When
        Usuario usuario = pacienteUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getPersona());
        assertEquals("Juan", usuario.getPersona().getNombres());
        assertEquals("Perez", usuario.getPersona().getApellidos());
        assertEquals("DNI", usuario.getPersona().getTipoDocumento());
        assertEquals("12345678", usuario.getPersona().getNumeroDocumento());
        assertEquals("1990-01-01", usuario.getPersona().getFechaNacimiento());
        assertEquals("M", usuario.getPersona().getGenero());
        assertEquals("123456789", usuario.getPersona().getTelefono());
        assertEquals("Test Address", usuario.getPersona().getDireccion());
        assertTrue(usuario.getPersona().getActivo());
    }

    @Test
    void testCrearUsuarioPacienteFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_PACIENTE")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = pacienteUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getPaciente());
        assertNotNull(usuario.getPaciente().getHistoriaClinica());
        assertEquals("A+", usuario.getPaciente().getGrupoSanguineo());
        assertEquals("+", usuario.getPaciente().getFactorRh());
        assertTrue(usuario.getPaciente().getActivo());
        assertNotNull(usuario.getPaciente().getFechaCreacion());
    }

    @Test
    void testCrearUsuarioRelationships() {
        // Given
        when(roleRepository.findByNombre("ROLE_PACIENTE")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = pacienteUser.crearUsuario(registerRequest);

        // Then
        assertEquals(usuario, usuario.getPersona().getUsuario());
        assertEquals(usuario, usuario.getPaciente().getUsuario());
    }
}
