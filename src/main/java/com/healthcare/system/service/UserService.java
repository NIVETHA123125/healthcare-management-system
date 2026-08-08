package com.healthcare.system.service;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.entity.User;
import com.healthcare.system.repository.PatientRepository;
import com.healthcare.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    public Optional<User> authenticate(String username, String password) {
        if (username != null) username = username.trim().toLowerCase();
        return userRepository.findByUsername(username)
                .filter(user -> checkPassword(password, user.getPassword()));
    }

    private boolean checkPassword(String plainPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            try {
                return org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, storedPassword);
            } catch (Exception e) {
                return false;
            }
        }
        return storedPassword.equals(plainPassword);
    }

    public User save(User user) {
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim().toLowerCase());
        }
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        if (username != null) username = username.trim().toLowerCase();
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        if (username != null) username = username.trim().toLowerCase();
        return userRepository.findByUsername(username).isPresent();
    }

    @Transactional
    public User registerPatient(String fullName, String email, String phone, String gender, 
                                 LocalDate dob, String address, String password) {
        // Create Patient Record
        Patient patient = new Patient();
        String firstName = fullName;
        String lastName = "";
        int spaceIndex = fullName.trim().indexOf(' ');
        if (spaceIndex > 0) {
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        }
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        if (email != null) email = email.trim().toLowerCase();
        patient.setEmail(email);
        patient.setPhone(phone);
        patient.setGender(gender);
        patient.setDob(dob);
        patient.setAddress(address);

        Patient savedPatient = patientRepository.saveAndFlush(patient);

        // Create Corresponding User Record
        User user = new User();
        user.setUsername(email); // For patients, email is username
        user.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
        user.setFullName(fullName);
        user.setRole("ROLE_PATIENT");
        user.setPatient(savedPatient);

        return userRepository.saveAndFlush(user);
    }

    public boolean verifyForgotPassword(String email, LocalDate dob) {
        if (email != null) email = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPatient() != null && user.getPatient().getDob() != null) {
                return user.getPatient().getDob().equals(dob);
            }
        }
        return false;
    }

    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        if (email != null) email = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt()));
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
