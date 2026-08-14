package com.healthcare.system.service;

import com.healthcare.system.entity.Appointment;
import com.healthcare.system.entity.Doctor;
import com.healthcare.system.entity.Patient;
import com.healthcare.system.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Appointment appointment1;
    private Appointment appointment2;
    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(10L);
        patient.setFirstName("Alice");

        doctor = new Doctor();
        doctor.setId(5L);
        doctor.setFirstName("Dr. Bob");

        appointment1 = new Appointment();
        appointment1.setId(100L);
        appointment1.setPatient(patient);
        appointment1.setDoctor(doctor);
        appointment1.setAppointmentDate(LocalDateTime.now().plusDays(1));
        appointment1.setStatus("PENDING");

        appointment2 = new Appointment();
        appointment2.setId(101L);
        appointment2.setPatient(patient);
        appointment2.setDoctor(doctor);
        appointment2.setAppointmentDate(LocalDateTime.now().plusDays(2));
        appointment2.setStatus("COMPLETED");
    }

    @Test
    @DisplayName("Should return all appointments ordered by date descending")
    void testGetAllAppointments() {
        when(appointmentRepository.findAllByOrderByAppointmentDateDesc()).thenReturn(Arrays.asList(appointment2, appointment1));

        List<Appointment> result = appointmentService.getAllAppointments();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(101L);
        verify(appointmentRepository, times(1)).findAllByOrderByAppointmentDateDesc();
    }

    @Test
    @DisplayName("Should return appointment by ID when present")
    void testGetAppointmentById() {
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment1));

        Optional<Appointment> result = appointmentService.getAppointmentById(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should calculate total scheduled appointments (PENDING + APPROVED)")
    void testGetScheduledCount() {
        when(appointmentRepository.countByStatus("PENDING")).thenReturn(3L);
        when(appointmentRepository.countByStatus("APPROVED")).thenReturn(2L);

        long scheduledCount = appointmentService.getScheduledCount();

        assertThat(scheduledCount).isEqualTo(5L);
        verify(appointmentRepository, times(1)).countByStatus("PENDING");
        verify(appointmentRepository, times(1)).countByStatus("APPROVED");
    }

    @Test
    @DisplayName("Should return appointments by doctor ID")
    void testGetAppointmentsByDoctor() {
        when(appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(5L))
                .thenReturn(Collections.singletonList(appointment1));

        List<Appointment> result = appointmentService.getAppointmentsByDoctor(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDoctor().getFirstName()).isEqualTo("Dr. Bob");
    }

    @Test
    @DisplayName("Should save appointment successfully")
    void testSaveAppointment() {
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment1);

        Appointment saved = appointmentService.saveAppointment(appointment1);

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        verify(appointmentRepository, times(1)).save(appointment1);
    }
}
