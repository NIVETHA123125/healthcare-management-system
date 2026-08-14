package com.healthcare.system.service;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.entity.User;
import com.healthcare.system.repository.PatientRepository;
import com.healthcare.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("user@example.com");
        // Hash for "password123"
        sampleUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("password123", org.mindrot.jbcrypt.BCrypt.gensalt()));
        sampleUser.setFullName("Test User");
        sampleUser.setRole("ROLE_PATIENT");
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void testAuthenticate_Success() {
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<User> authenticated = userService.authenticate("user@example.com", "password123");

        assertThat(authenticated).isPresent();
        assertThat(authenticated.get().getUsername()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should fail authentication with invalid password")
    void testAuthenticate_WrongPassword() {
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<User> authenticated = userService.authenticate("user@example.com", "wrongpass");

        assertThat(authenticated).isEmpty();
    }

    @Test
    @DisplayName("Should register patient and create associated User entity")
    void testRegisterPatient() {
        Patient savedPatient = new Patient();
        savedPatient.setId(10L);
        savedPatient.setFirstName("John");
        savedPatient.setLastName("Doe");
        savedPatient.setEmail("john@example.com");

        when(patientRepository.saveAndFlush(any(Patient.class))).thenReturn(savedPatient);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerPatient(
                "John Doe",
                "john@example.com",
                "1234567890",
                "Male",
                LocalDate.of(1995, 3, 10),
                "123 Main St",
                "secretPassword"
        );

        assertThat(user).isNotNull();
        assertThat(user.getRole()).isEqualTo("ROLE_PATIENT");
        assertThat(user.getUsername()).isEqualTo("john@example.com");
        assertThat(user.getPatient()).isEqualTo(savedPatient);

        verify(patientRepository, times(1)).saveAndFlush(any(Patient.class));
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("Should verify existsByUsername ignoring case")
    void testExistsByUsername() {
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(sampleUser));

        boolean exists = userService.existsByUsername(" USER@EXAMPLE.COM ");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should reset password successfully when user exists")
    void testResetPassword_Success() {
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(sampleUser));

        boolean result = userService.resetPassword("user@example.com", "newSecretPassword");

        assertThat(result).isTrue();
        verify(userRepository, times(1)).save(sampleUser);
    }
}
