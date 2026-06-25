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
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
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
        patient.setEmail(email);
        patient.setPhone(phone);
        patient.setGender(gender);
        patient.setDob(dob);
        patient.setAddress(address);

        Patient savedPatient = patientRepository.save(patient);

        // Create Corresponding User Record
        User user = new User();
        user.setUsername(email); // For patients, email is username
        user.setPassword(password);
        user.setFullName(fullName);
        user.setRole("ROLE_PATIENT");
        user.setPatient(savedPatient);

        return userRepository.save(user);
    }

    public boolean verifyForgotPassword(String email, LocalDate dob) {
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
        Optional<User> userOpt = userRepository.findByUsername(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
