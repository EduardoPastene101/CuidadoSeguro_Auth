package com.hospital.authservice.factory;

import com.hospital.authservice.dto.RegisterRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdminUser adminUser;

    private RegisterRequest registerRequest;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .nombre("ROLE_ADMIN")
                .activo(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("admin")
                .password("Password123!")
                .email("admin@test.com")
                .tipoUsuario("ADMIN")
                .nombres("Admin")
                .apellidos("User")
                .tipoDocumento("DNI")
                .numeroDocumento("12345678")
                .roles(List.of("ROLE_ADMIN"))
                .build();
    }

    @Test
    void testCrearUsuarioSuccess() {
        // Given
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(role));

        // When
        Usuario usuario = adminUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertEquals("admin", usuario.getUsername());
        assertEquals("Password123!", usuario.getPassword());
        assertEquals("admin@test.com", usuario.getEmail());
        assertTrue(usuario.getActivo());
        assertTrue(usuario.getNoBloqueado());
        assertNotNull(usuario.getPersona());
        assertNotNull(usuario.getRoles());
        assertEquals(1, usuario.getRoles().size());
        verify(roleRepository).findByNombre("ROLE_ADMIN");
    }

    @Test
    void testCrearUsuarioWithMultipleRoles() {
        // Given
        Role role2 = Role.builder()
                .id(2L)
                .nombre("ROLE_USER")
                .activo(true)
                .build();

        registerRequest.setRoles(List.of("ROLE_ADMIN", "ROLE_USER"));
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.findByNombre("ROLE_USER")).thenReturn(Optional.of(role2));

        // When
        Usuario usuario = adminUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario);
        assertEquals(2, usuario.getRoles().size());
        verify(roleRepository).findByNombre("ROLE_ADMIN");
        verify(roleRepository).findByNombre("ROLE_USER");
    }

    @Test
    void testCrearUsuarioRoleNotFound() {
        // Given
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> adminUser.crearUsuario(registerRequest));
        verify(roleRepository).findByNombre("ROLE_ADMIN");
    }

    @Test
    void testValidarDatosEspecificosSuccess() {
        // Given
        registerRequest.setRoles(List.of("ROLE_ADMIN"));

        // When & Then
        assertDoesNotThrow(() -> adminUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testValidarDatosEspecificosNoRoles() {
        // Given
        registerRequest.setRoles(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("Los roles son obligatorios para usuarios administradores", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosEmptyRoles() {
        // Given
        registerRequest.setRoles(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("Los roles son obligatorios para usuarios administradores", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosMissingAdminRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER"));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminUser.validarDatosEspecificos(registerRequest)
        );
        assertEquals("El usuario administrador debe tener el rol ROLE_ADMIN", exception.getMessage());
    }

    @Test
    void testValidarDatosEspecificosWithAdminRole() {
        // Given
        registerRequest.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

        // When & Then
        assertDoesNotThrow(() -> adminUser.validarDatosEspecificos(registerRequest));
    }

    @Test
    void testGetTipoUsuario() {
        // When
        String tipoUsuario = adminUser.getTipoUsuario();

        // Then
        assertEquals("ADMIN", tipoUsuario);
    }

    @Test
    void testCrearUsuarioPersonaFields() {
        // Given
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(role));
        registerRequest.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        registerRequest.setGenero("M");
        registerRequest.setTelefono("123456789");
        registerRequest.setDireccion("Test Address");

        // When
        Usuario usuario = adminUser.crearUsuario(registerRequest);

        // Then
        assertNotNull(usuario.getPersona());
        assertEquals("Admin", usuario.getPersona().getNombres());
        assertEquals("User", usuario.getPersona().getApellidos());
        assertEquals("DNI", usuario.getPersona().getTipoDocumento());
        assertEquals("12345678", usuario.getPersona().getNumeroDocumento());
        assertEquals("1990-01-01", usuario.getPersona().getFechaNacimiento());
        assertEquals("M", usuario.getPersona().getGenero());
        assertEquals("123456789", usuario.getPersona().getTelefono());
        assertEquals("Test Address", usuario.getPersona().getDireccion());
        assertTrue(usuario.getPersona().getActivo());
    }
}
