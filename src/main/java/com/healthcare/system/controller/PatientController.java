package com.healthcare.system.controller;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.service.PatientService;
import com.healthcare.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

import com.healthcare.system.service.NotificationService;
import com.healthcare.system.repository.UserRepository;
import com.healthcare.system.repository.PatientRepository;
import com.healthcare.system.entity.User;

@Controller
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String listPatients(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "regDate", required = false) String regDateStr,
            @RequestParam(value = "ageGroup", required = false) String ageGroup,
            Model model) {
        
        java.time.LocalDate regDate = null;
        if (regDateStr != null && !regDateStr.trim().isEmpty()) {
            try {
                regDate = java.time.LocalDate.parse(regDateStr);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        
        Integer minAge = null;
        Integer maxAge = null;
        if (ageGroup != null && !ageGroup.trim().isEmpty()) {
            if ("0-17".equals(ageGroup)) { minAge = 0; maxAge = 17; }
            else if ("18-35".equals(ageGroup)) { minAge = 18; maxAge = 35; }
            else if ("36-50".equals(ageGroup)) { minAge = 36; maxAge = 50; }
            else if ("51-65".equals(ageGroup)) { minAge = 51; maxAge = 65; }
            else if ("66-150".equals(ageGroup)) { minAge = 66; maxAge = 150; }
        }
        
        List<Patient> patients = patientRepository.searchAndFilterPatients(
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            gender != null && !gender.trim().isEmpty() ? gender.trim() : null,
            regDate,
            minAge,
            maxAge
        );
        
        model.addAttribute("patients", patients);
        model.addAttribute("search", search);
        model.addAttribute("gender", gender);
        model.addAttribute("regDate", regDateStr);
        model.addAttribute("ageGroup", ageGroup);
        return "patients";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("patient", new Patient());
        model.addAttribute("pageTitle", "Register New Patient");
        return "patient-form";
    }

    @PostMapping("/save")
    public String savePatient(@ModelAttribute("patient") Patient patient, Model model, RedirectAttributes redirectAttributes) {
        if (patient.getEmail() != null) {
            patient.setEmail(patient.getEmail().trim().toLowerCase());
        }
        if (patient.getPhone() != null) {
            patient.setPhone(patient.getPhone().trim());
        }
        
        boolean hasErrors = false;

        // 1. Name validation
        if (patient.getFirstName() == null || patient.getFirstName().trim().isEmpty()) {
            model.addAttribute("firstNameError", "First Name cannot be empty.");
            hasErrors = true;
        }
        if (patient.getLastName() == null || patient.getLastName().trim().isEmpty()) {
            model.addAttribute("lastNameError", "Last Name cannot be empty.");
            hasErrors = true;
        }

        // 2. Email format validation
        if (patient.getEmail() == null || !patient.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            model.addAttribute("emailError", "Please enter a valid email address.");
            hasErrors = true;
        } else {
            // Duplicate Email Check
            Optional<Patient> existingEmail = patient.getId() == null
                ? patientService.findByEmail(patient.getEmail())
                : patientService.findByEmailAndIdNot(patient.getEmail(), patient.getId());
            
            // Also check users table to prevent duplicate username logins
            boolean emailExistsInUsers = patient.getId() == null 
                ? userService.existsByUsername(patient.getEmail())
                : userService.findByUsername(patient.getEmail())
                    .filter(u -> u.getPatient() == null || !u.getPatient().getId().equals(patient.getId())).isPresent();

            if (existingEmail.isPresent() || emailExistsInUsers) {
                model.addAttribute("emailError", "Email already exists.");
                hasErrors = true;
            }
        }

        // 3. Phone number validation
        if (patient.getPhone() == null || !patient.getPhone().matches("^[6-9]\\d{9}$")) {
            model.addAttribute("phoneError", "Please enter a valid 10-digit phone number.");
            hasErrors = true;
        } else {
            // Duplicate Phone Check
            Optional<Patient> existingPhone = patient.getId() == null
                ? patientService.findByPhone(patient.getPhone())
                : patientService.findByPhoneAndIdNot(patient.getPhone(), patient.getId());
            if (existingPhone.isPresent()) {
                model.addAttribute("phoneError", "Phone number already registered.");
                hasErrors = true;
            }
        }

        if (hasErrors) {
            model.addAttribute("patient", patient);
            model.addAttribute("pageTitle", patient.getId() == null ? "Register New Patient" : "Edit Patient Details");
            return "patient-form";
        }

        try {
            boolean isNew = (patient.getId() == null);
            patientService.savePatient(patient);
            
            if (isNew) {
                // If there's no user record with this email, create one
                User patUser = null;
                if (userRepository.findByUsername(patient.getEmail()).isEmpty()) {
                    patUser = new User();
                    patUser.setUsername(patient.getEmail().toLowerCase());
                    patUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("patient123", org.mindrot.jbcrypt.BCrypt.gensalt()));
                    patUser.setFullName(patient.getFirstName() + " " + patient.getLastName());
                    patUser.setRole("ROLE_PATIENT");
                    patUser.setPatient(patient);
                    patUser = userService.save(patUser);
                } else {
                    patUser = userRepository.findByUsername(patient.getEmail()).orElse(null);
                }
                
                // Patient Notification
                if (patUser != null) {
                    notificationService.createNotification(patUser, "PATIENT", "Welcome to CareGrid", 
                        "Your patient account has been successfully registered by Staff.", "ACCOUNT", patient.getId());
                }
                
                // Admin Notification
                notificationService.createNotification(null, "ADMIN", "New Patient Registration", 
                    "A new patient was registered by Staff: " + patient.getFirstName() + " " + patient.getLastName() + " (" + patient.getEmail() + ")", 
                    "SYSTEM", patient.getId());

                redirectAttributes.addFlashAttribute("successMessage", "Patient registered successfully! Default login password is patient123.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Patient records updated successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving patient: " + e.getMessage());
        }
        return "redirect:/patients";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Patient patient = patientService.getPatientById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient Id:" + id));
        model.addAttribute("patient", patient);
        model.addAttribute("pageTitle", "Edit Patient Details");
        return "patient-form";
    }

    @GetMapping("/delete/{id}")
    public String deletePatient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            patientService.deletePatient(id);
            redirectAttributes.addFlashAttribute("successMessage", "Patient records deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting patient: " + e.getMessage());
        }
        return "redirect:/patients";
    }
}
