package com.healthcare.system.service;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient patient1;
    private Patient patient2;

    @BeforeEach
    void setUp() {
        patient1 = new Patient();
        patient1.setId(1L);
        patient1.setFirstName("John");
        patient1.setLastName("Doe");
        patient1.setEmail("john.doe@example.com");
        patient1.setPhone("1234567890");
        patient1.setGender("Male");
        patient1.setDob(LocalDate.of(1990, 1, 15));

        patient2 = new Patient();
        patient2.setId(2L);
        patient2.setFirstName("Jane");
        patient2.setLastName("Smith");
        patient2.setEmail("jane.smith@example.com");
        patient2.setPhone("0987654321");
        patient2.setGender("Female");
        patient2.setDob(LocalDate.of(1995, 5, 20));
    }

    @Test
    @DisplayName("Should return all patients successfully")
    void testGetAllPatients() {
        when(patientRepository.findAll()).thenReturn(Arrays.asList(patient1, patient2));

        List<Patient> result = patientService.getAllPatients();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(1).getFirstName()).isEqualTo("Jane");
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return patient by ID when found")
    void testGetPatientById_Found() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient1));

        Optional<Patient> result = patientService.getPatientById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("John Doe");
        verify(patientRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty optional when patient ID is not found")
    void testGetPatientById_NotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Patient> result = patientService.getPatientById(99L);

        assertThat(result).isEmpty();
        verify(patientRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Should save and return patient successfully")
    void testSavePatient() {
        when(patientRepository.save(any(Patient.class))).thenReturn(patient1);

        Patient saved = patientService.savePatient(patient1);

        assertThat(saved).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
        verify(patientRepository, times(1)).save(patient1);
    }

    @Test
    @DisplayName("Should search patients by keyword")
    void testSearchPatients() {
        when(patientRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                "John", "John", "John"))
                .thenReturn(Collections.singletonList(patient1));

        List<Patient> result = patientService.searchPatients("John");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return all patients when search keyword is blank")
    void testSearchPatients_BlankKeyword() {
        when(patientRepository.findAll()).thenReturn(Arrays.asList(patient1, patient2));

        List<Patient> result = patientService.searchPatients("  ");

        assertThat(result).hasSize(2);
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should cascade delete patient records cleanly")
    void testDeletePatient() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient1));
        when(prescriptionRepository.findByPatientIdOrderByPrescribedDateDesc(1L)).thenReturn(Collections.emptyList());
        when(consultationRepository.findByPatientIdOrderByCreatedDateDesc(1L)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByPatientIdOrderByPaymentDateDesc(1L)).thenReturn(Collections.emptyList());
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(1L)).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(patient1.getEmail())).thenReturn(Optional.empty());

        patientService.deletePatient(1L);

        verify(patientRepository, times(1)).delete(patient1);
    }
}
