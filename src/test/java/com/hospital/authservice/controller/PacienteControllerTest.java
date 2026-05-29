package com.hospital.authservice.controller;

import com.hospital.authservice.entity.Paciente;
import com.hospital.authservice.entity.Persona;
import com.hospital.authservice.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteControllerTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteController pacienteController;

    private Paciente paciente;
    private Persona persona;

    @BeforeEach
    void setUp() {
        persona = Persona.builder()
                .id(1L)
                .nombres("Juan")
                .apellidos("Perez")
                .tipoDocumento("DNI")
                .numeroDocumento("12345678")
                .activo(true)
                .build();

        paciente = Paciente.builder()
                .id(1L)
                .historiaClinica("HC-001")
                .grupoSanguineo("A+")
                .factorRh("+")
                .activo(true)
                .persona(persona)
                .build();
    }

    @Test
    void testObtenerPorRutSuccess() {
        // Given
        String rut = "12345678";
        when(pacienteRepository.findByPersona_NumeroDocumento(rut))
                .thenReturn(Optional.of(paciente));

        // When
        var response = pacienteController.obtenerPorRut(rut);

        // Then
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(paciente, response.getBody());
        verify(pacienteRepository).findByPersona_NumeroDocumento(rut);
    }

    @Test
    void testObtenerPorRutNotFound() {
        // Given
        String rut = "99999999";
        when(pacienteRepository.findByPersona_NumeroDocumento(rut))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> pacienteController.obtenerPorRut(rut));
        verify(pacienteRepository).findByPersona_NumeroDocumento(rut);
    }

    @Test
    void testObtenerPorRutWithDifferentRut() {
        // Given
        String rut = "87654321";
        when(pacienteRepository.findByPersona_NumeroDocumento(rut))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> pacienteController.obtenerPorRut(rut));
        verify(pacienteRepository).findByPersona_NumeroDocumento(rut);
    }
}
