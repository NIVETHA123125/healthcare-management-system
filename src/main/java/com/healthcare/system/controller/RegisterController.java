package com.healthcare.system.controller;

import com.healthcare.system.service.UserService;
import com.healthcare.system.service.DoctorService;
import com.healthcare.system.repository.PatientRepository;
import com.healthcare.system.entity.Doctor;
import com.healthcare.system.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

import com.healthcare.system.service.NotificationService;
import com.healthcare.system.service.EmailService;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.healthcare.system.service.OtpService otpService;

    @GetMapping("/register")
    public String registerPage(Model model) {
        return "register";
    }

    @GetMapping("/register-doctor")
    public String registerDoctorPage(Model model) {
        return "redirect:/login";
    }

    @PostMapping("/register/save")
    public String registerPatient(@RequestParam String fullName,
                                  @RequestParam String email,
                                  @RequestParam String phone,
                                  @RequestParam String gender,
                                  @RequestParam String dob,
                                  @RequestParam String address,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  Model model) {
        
        // Trim inputs and normalize email to lowercase to prevent duplicate key violations due to casing/spaces
        if (fullName != null) fullName = fullName.trim();
        if (email != null) email = email.trim().toLowerCase();
        if (phone != null) phone = phone.trim();
        if (address != null) address = address.trim();
        
        boolean hasErrors = false;

        // Backend OTP verification verification
        boolean isVerified = false;
        try {
            isVerified = otpService.markOtpAsUsed(email);
        } catch (Exception e) {
            isVerified = false;
        }

        if (!isVerified) {
            model.addAttribute("emailError", "Please complete email verification using OTP first.");
            hasErrors = true;
        }

        // 1. Name validation
        if (fullName == null || fullName.trim().isEmpty()) {
            model.addAttribute("fullNameError", "Name cannot be empty.");
            hasErrors = true;
        }

        // 2. Email format validation
        if (email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            model.addAttribute("emailError", "Please enter a valid email address.");
            hasErrors = true;
        } else if (userService.existsByUsername(email) || patientRepository.findByEmail(email).isPresent()) {
            // Duplicate email check
            model.addAttribute("emailError", "Email already exists.");
            hasErrors = true;
        }

        // 3. Phone number validation
        if (phone == null || !phone.matches("^[6-9]\\d{9}$")) {
            model.addAttribute("phoneError", "Please enter a valid 10-digit phone number.");
            hasErrors = true;
        } else if (patientRepository.findByPhone(phone).isPresent()) {
            // Duplicate phone check
            model.addAttribute("phoneError", "Phone number already registered.");
            hasErrors = true;
        }

        // 4. Password validation
        if (password == null || password.length() < 8) {
            model.addAttribute("passwordError", "Password must have a minimum length of 8 characters.");
            hasErrors = true;
        }

        // 5. Confirm password validation
        if (password != null && !password.equals(confirmPassword)) {
            model.addAttribute("confirmPasswordError", "Passwords do not match.");
            hasErrors = true;
        }

        if (hasErrors) {
            // Pre-populate fields on validation error
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("gender", gender);
            model.addAttribute("dob", dob);
            model.addAttribute("address", address);
            return "register";
        }

        try {
            LocalDate parsedDob = LocalDate.parse(dob);
            User savedUser = userService.registerPatient(fullName, email, phone, gender, parsedDob, address, password);
            
            // Patient notification
            notificationService.createNotification(savedUser, "PATIENT", "Welcome to CareGrid", 
                "Your patient account has been successfully registered.", "ACCOUNT", savedUser.getPatient().getId());
            
            // Admin notification
            notificationService.createNotification(null, "ADMIN", "New Patient Registration", 
                "A new patient has self-registered: " + fullName + " (" + email + ")", "SYSTEM", savedUser.getPatient().getId());
                
            return "redirect:/login?registrationSuccess=true";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to register: " + e.getMessage());
            // Pre-populate fields on error
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("gender", gender);
            model.addAttribute("dob", dob);
            model.addAttribute("address", address);
            return "register";
        }
    }

    @PostMapping("/register-doctor/save")
    public String registerDoctor() {
        return "redirect:/login";
    }
}
