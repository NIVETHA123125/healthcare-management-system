package com.healthcare.system.controller;

import com.healthcare.system.entity.Doctor;
import com.healthcare.system.entity.User;
import com.healthcare.system.service.DoctorService;
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

import com.healthcare.system.repository.DoctorRepository;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String listDoctors(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "spec", required = false) String spec,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "expRange", required = false) String expRange,
            Model model) {
        
        Integer minExp = null;
        Integer maxExp = null;
        if (expRange != null && !expRange.trim().isEmpty()) {
            if ("0-4".equals(expRange)) { minExp = 0; maxExp = 4; }
            else if ("5-10".equals(expRange)) { minExp = 5; maxExp = 10; }
            else if ("11-20".equals(expRange)) { minExp = 11; maxExp = 20; }
            else if ("21-100".equals(expRange)) { minExp = 21; maxExp = 100; }
        }
        
        List<Doctor> doctors = doctorRepository.searchAndFilterDoctors(
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            spec != null && !spec.trim().isEmpty() ? spec.trim() : null,
            status != null && !status.trim().isEmpty() ? status.trim() : null,
            minExp,
            maxExp
        );
        
        // Fetch unique specializations to populate the dropdown
        List<String> specializations = doctorRepository.findAll().stream()
            .map(Doctor::getSpecialization)
            .filter(s -> s != null && !s.trim().isEmpty())
            .distinct()
            .toList();
            
        model.addAttribute("doctors", doctors);
        model.addAttribute("search", search);
        model.addAttribute("spec", spec);
        model.addAttribute("status", status);
        model.addAttribute("expRange", expRange);
        model.addAttribute("specializations", specializations);
        return "doctors";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        model.addAttribute("pageTitle", "Register New Doctor");
        return "doctor-form";
    }

    @PostMapping("/save")
    public String saveDoctor(@ModelAttribute("doctor") Doctor doctor,
                             @RequestParam(value = "password", required = false) String password,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (doctor.getEmail() != null) {
            doctor.setEmail(doctor.getEmail().trim().toLowerCase());
        }
        if (doctor.getPhone() != null) {
            doctor.setPhone(doctor.getPhone().trim());
        }
        
        boolean hasErrors = false;

        // 1. Email format validation
        if (doctor.getEmail() == null || !doctor.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            model.addAttribute("emailError", "Please enter a valid email address.");
            hasErrors = true;
        } else {
            // Duplicate Email Check
            Optional<Doctor> existingEmail = doctor.getId() == null 
                ? doctorService.findByEmail(doctor.getEmail())
                : doctorService.findByEmailAndIdNot(doctor.getEmail(), doctor.getId());
            if (existingEmail.isPresent()) {
                model.addAttribute("emailError", "Email already exists.");
                hasErrors = true;
            }
        }

        // 2. Phone number validation
        if (doctor.getPhone() == null || !doctor.getPhone().matches("^[6-9]\\d{9}$")) {
            model.addAttribute("phoneError", "Please enter a valid 10-digit phone number.");
            hasErrors = true;
        } else {
            // Duplicate Phone Check
            Optional<Doctor> existingPhone = doctor.getId() == null
                ? doctorService.findByPhone(doctor.getPhone())
                : doctorService.findByPhoneAndIdNot(doctor.getPhone(), doctor.getId());
            if (existingPhone.isPresent()) {
                model.addAttribute("phoneError", "Phone number already registered.");
                hasErrors = true;
            }
        }

        // 3. Experience validation (non-negative)
        if (doctor.getExperience() != null && doctor.getExperience() < 0) {
            model.addAttribute("experienceError", "Experience cannot be negative.");
            hasErrors = true;
        }

        // 4. Consultation Fee validation (non-negative)
        if (doctor.getConsultationFee() != null && doctor.getConsultationFee() < 0.0) {
            model.addAttribute("consultationFeeError", "Consultation Fee cannot be negative.");
            hasErrors = true;
        }

        if (hasErrors) {
            model.addAttribute("doctor", doctor);
            model.addAttribute("pageTitle", doctor.getId() == null ? "Register New Doctor" : "Edit Doctor Details");
            return "doctor-form";
        }

        try {
            boolean isNew = (doctor.getId() == null);
            String oldStatus = null;
            if (!isNew) {
                oldStatus = doctorService.getDoctorById(doctor.getId())
                    .map(Doctor::getAvailabilityStatus)
                    .orElse(null);
            }

            doctorService.saveDoctor(doctor);
            
            // Sync with User table
            User docUser = userRepository.findByUsername(doctor.getEmail().toLowerCase()).orElse(null);
            
            // If not found by email, try finding by doctor entity relation
            if (docUser == null && doctor.getId() != null) {
                docUser = userRepository.findAll().stream()
                    .filter(u -> u.getDoctor() != null && u.getDoctor().getId().equals(doctor.getId()))
                    .findFirst()
                    .orElse(null);
            }
            
            if (docUser == null) {
                docUser = new User();
                docUser.setUsername(doctor.getEmail().toLowerCase());
                String finalPassword = (password != null && !password.trim().isEmpty()) ? password : "Doctor@123";
                docUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(finalPassword, org.mindrot.jbcrypt.BCrypt.gensalt()));
                docUser.setFullName("Dr. " + doctor.getFullName());
                docUser.setRole("ROLE_DOCTOR");
                docUser.setDoctor(doctor);
                docUser = userService.save(docUser);
            } else {
                // Update existing user details to match updated doctor details
                docUser.setUsername(doctor.getEmail().toLowerCase());
                docUser.setFullName("Dr. " + doctor.getFullName());
                docUser.setRole("ROLE_DOCTOR");
                docUser.setDoctor(doctor);
                docUser = userService.save(docUser);
            }
            
            if (isNew) {
                // Doctor Notification
                notificationService.createNotification(docUser, "DOCTOR", "Welcome to CareGrid", 
                    "Your doctor portal account has been successfully registered by Staff.", "ACCOUNT", doctor.getId());
                
                // Admin Notification
                notificationService.createNotification(null, "ADMIN", "New Doctor Created", 
                    "Staff has registered a new doctor: Dr. " + doctor.getFullName(), "SYSTEM", doctor.getId());

                redirectAttributes.addFlashAttribute("successMessage", "Doctor registered successfully! Default login password is Doctor@123.");
            } else {
                // Check for status changes
                if (oldStatus != null && !oldStatus.equalsIgnoreCase(doctor.getAvailabilityStatus())) {
                    if ("Available".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
                        notificationService.createNotification(docUser, "DOCTOR", "Account Activated", 
                            "Your practitioner profile has been activated and is now visible in the directory.", "ACCOUNT", doctor.getId());
                        notificationService.createNotification(null, "ADMIN", "Doctor Account Activated", 
                            "Doctor profile Dr. " + doctor.getFullName() + " has been activated.", "SYSTEM", doctor.getId());
                    } else if ("Unavailable".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
                        notificationService.createNotification(docUser, "DOCTOR", "Account Deactivated", 
                            "Your practitioner profile availability status has been marked as Unavailable.", "ACCOUNT", doctor.getId());
                        notificationService.createNotification(null, "ADMIN", "Doctor Account Deactivated", 
                            "Doctor profile Dr. " + doctor.getFullName() + " has been deactivated.", "SYSTEM", doctor.getId());
                    }
                }
                redirectAttributes.addFlashAttribute("successMessage", "Doctor profile updated successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving doctor details: " + e.getMessage());
        }
        return "redirect:/doctors";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Doctor doctor = doctorService.getDoctorById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid doctor Id:" + id));
        model.addAttribute("doctor", doctor);
        model.addAttribute("pageTitle", "Edit Doctor Details");
        return "doctor-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteDoctor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            doctorService.deleteDoctor(id);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting doctor: " + e.getMessage());
        }
        return "redirect:/doctors";
    }
}
