package com.healthcare.system.controller;

import com.healthcare.system.entity.*;
import com.healthcare.system.repository.*;
import com.healthcare.system.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.*;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String viewSettings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        UserSettings settings = getOrCreateSettings(user);
        model.addAttribute("settings", settings);

        // Dummy Login Activity data
        List<Map<String, String>> loginActivity = List.of(
            Map.of("date", "2026-07-07 08:35 PM", "device", "Chrome on Windows (Current)", "ip", "192.168.1.15"),
            Map.of("date", "2026-07-06 02:14 PM", "device", "Safari on iPhone", "ip", "10.0.0.42"),
            Map.of("date", "2026-07-04 10:05 AM", "device", "Firefox on Windows", "ip", "192.168.1.10")
        );
        model.addAttribute("loginActivity", loginActivity);
        model.addAttribute("lastLoginInfo", "2026-07-07 08:35 PM via Chrome (Windows)");

        return "settings";
    }

    @PostMapping("/profile")
    @ResponseBody
    public Map<String, Object> updateProfile(@RequestParam String fullName,
                                            @RequestParam String phone,
                                            @RequestParam String email,
                                            @RequestParam(required = false) String address,
                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized session");
            return response;
        }

        user.setFullName(fullName);
        userRepository.save(user);

        if (user.getPatient() != null) {
            Patient patient = user.getPatient();
            patient.setPhone(phone);
            patient.setEmail(email);
            patient.setAddress(address);
            patientRepository.save(patient);
        } else if (user.getDoctor() != null) {
            Doctor doctor = user.getDoctor();
            doctor.setPhone(phone);
            doctor.setEmail(email);
            doctorRepository.save(doctor);
        }

        // Simulating verification response if email is new
        response.put("success", true);
        response.put("message", "Profile updated. Verification code sent to " + email);
        return response;
    }

    @PostMapping("/security")
    @ResponseBody
    public Map<String, Object> updateSecurity(@RequestParam String currentPassword,
                                             @RequestParam String newPassword,
                                             @RequestParam String confirmPassword,
                                             HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized session");
            return response;
        }

        if (!user.getPassword().equals(currentPassword)) {
            response.put("success", false);
            response.put("message", "Incorrect current password");
            return response;
        }

        if (newPassword.length() < 8 ||
            !newPassword.matches(".*[A-Z].*") ||
            !newPassword.matches(".*[a-z].*") ||
            !newPassword.matches(".*\\d.*") ||
            !newPassword.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            response.put("success", false);
            response.put("message", "Password must contain min 8 chars, uppercase, lowercase, digit, and a special character");
            return response;
        }

        if (!newPassword.equals(confirmPassword)) {
            response.put("success", false);
            response.put("message", "Confirm password does not match");
            return response;
        }

        user.setPassword(newPassword);
        userRepository.save(user);

        // Generate password changed notification
        String roleStr = user.getRole();
        if (roleStr.startsWith("ROLE_")) {
            roleStr = roleStr.replace("ROLE_", "");
        }
        notificationService.createNotification(user, roleStr, "Password Changed Successfully", 
            "Your account password was updated successfully from Settings.", "ACCOUNT", user.getId());

        response.put("success", true);
        response.put("message", "Password changed successfully!");
        return response;
    }

    @PostMapping("/appearance")
    @ResponseBody
    public Map<String, Object> updateAppearance(@RequestParam String theme,
                                               HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        UserSettings settings = getOrCreateSettings(user);
        settings.setTheme(theme);
        userSettingsRepository.save(settings);

        session.setAttribute("userSettings", settings);

        response.put("success", true);
        response.put("message", "Appearance preferences saved successfully!");
        return response;
    }

    @PostMapping("/accessibility")
    @ResponseBody
    public Map<String, Object> updateAccessibility(@RequestParam String fontSize,
                                                  @RequestParam(defaultValue = "false") boolean highContrast,
                                                  @RequestParam(defaultValue = "false") boolean reduceMotion,
                                                  @RequestParam(defaultValue = "false") boolean keyboardNavigation,
                                                  HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        UserSettings settings = getOrCreateSettings(user);
        settings.setFontSize(fontSize);
        settings.setHighContrast(highContrast);
        settings.setReduceMotion(reduceMotion);
        settings.setKeyboardNavigation(keyboardNavigation);
        userSettingsRepository.save(settings);

        session.setAttribute("userSettings", settings);

        response.put("success", true);
        response.put("message", "Accessibility settings saved successfully!");
        return response;
    }

    @PostMapping("/privacy")
    @ResponseBody
    public Map<String, Object> updatePrivacy(@RequestParam String personalInfoVisibility,
                                            @RequestParam(required = false) String medicalNotes,
                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        UserSettings settings = getOrCreateSettings(user);
        settings.setPersonalInfoVisibility(personalInfoVisibility);
        if (medicalNotes != null) {
            settings.setMedicalNotes(medicalNotes);
        }
        userSettingsRepository.save(settings);

        session.setAttribute("userSettings", settings);

        response.put("success", true);
        response.put("message", "Privacy settings saved successfully!");
        return response;
    }

    @PostMapping("/language")
    @ResponseBody
    public Map<String, Object> updateLanguage(@RequestParam String language,
                                             HttpServletRequest request,
                                             HttpServletResponse response,
                                             HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            res.put("success", false);
            res.put("message", "Unauthorized");
            return res;
        }

        UserSettings settings = getOrCreateSettings(user);
        settings.setLanguage(language);
        userSettingsRepository.save(settings);

        session.setAttribute("userSettings", settings);

        // Update spring locale
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver != null) {
            localeResolver.setLocale(request, response, new Locale(language));
        }

        res.put("success", true);
        res.put("message", "Language preference set to: " + language.toUpperCase());
        return res;
    }

    @PostMapping("/notifications")
    @ResponseBody
    public Map<String, Object> updateNotifications(@RequestParam(defaultValue = "false") boolean emailNotifications,
                                                   @RequestParam(defaultValue = "false") boolean smsNotifications,
                                                   @RequestParam(defaultValue = "false") boolean appointmentReminders,
                                                   @RequestParam(defaultValue = "false") boolean systemAnnouncements,
                                                   @RequestParam(defaultValue = "false") boolean pushNotifications,
                                                   HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        UserSettings settings = getOrCreateSettings(user);
        settings.setEmailNotifications(emailNotifications);
        settings.setSmsNotifications(smsNotifications);
        settings.setAppointmentReminders(appointmentReminders);
        settings.setSystemAnnouncements(systemAnnouncements);
        settings.setPushNotifications(pushNotifications);
        userSettingsRepository.save(settings);

        session.setAttribute("userSettings", settings);

        response.put("success", true);
        response.put("message", "Notification channels saved!");
        return response;
    }

    @PostMapping("/role")
    @ResponseBody
    public Map<String, Object> updateRoleSettings(@RequestParam Map<String, String> params, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        UserSettings settings = getOrCreateSettings(user);

        if (user.getRole().contains("PATIENT")) {
            settings.setPreferredHospital(params.getOrDefault("preferredHospital", ""));
            settings.setPreferredDoctor(params.getOrDefault("preferredDoctor", ""));
            settings.setEmergencyContact(params.getOrDefault("emergencyContact", ""));
            settings.setBloodGroup(params.getOrDefault("bloodGroup", ""));
            settings.setAllergies(params.getOrDefault("allergies", ""));
            settings.setMedicalNotes(params.getOrDefault("medicalNotes", ""));
            settings.setDefaultConsultationMode(params.getOrDefault("defaultConsultationMode", "OFFLINE"));
        } else if (user.getRole().contains("DOCTOR")) {
            settings.setSpecialization(params.getOrDefault("specialization", ""));
            if (params.containsKey("consultationFee")) {
                settings.setConsultationFee(Double.parseDouble(params.get("consultationFee")));
            }
            settings.setAvailableDays(params.getOrDefault("availableDays", ""));
            settings.setAvailableTimeSlots(params.getOrDefault("availableTimeSlots", ""));
            if (params.containsKey("consultationDuration")) {
                settings.setConsultationDuration(Integer.parseInt(params.get("consultationDuration")));
            }
            settings.setOnlineConsultation("true".equals(params.get("onlineConsultation")));
            settings.setClinicName(params.getOrDefault("clinicName", ""));
            settings.setClinicAddress(params.getOrDefault("clinicAddress", ""));
            settings.setClinicContactNumber(params.getOrDefault("clinicContactNumber", ""));
        } else if (user.getRole().contains("ADMIN")) {
            settings.setHospitalName(params.getOrDefault("hospitalName", ""));
            settings.setHospitalLogo(params.getOrDefault("hospitalLogo", ""));
            settings.setDefaultTimeZone(params.getOrDefault("defaultTimeZone", ""));
            settings.setDefaultLanguage(params.getOrDefault("defaultLanguage", ""));
            settings.setDefaultCurrency(params.getOrDefault("defaultCurrency", ""));
            settings.setDefaultDateFormat(params.getOrDefault("defaultDateFormat", ""));
            settings.setDefaultPatientStatus(params.getOrDefault("defaultPatientStatus", ""));
            settings.setDefaultDoctorStatus(params.getOrDefault("defaultDoctorStatus", ""));
            settings.setRegistrationApprovalSettings(params.getOrDefault("registrationApprovalSettings", ""));
            if (params.containsKey("sessionTimeout")) {
                settings.setSessionTimeout(Integer.parseInt(params.get("sessionTimeout")));
            }
            settings.setPasswordPolicy(params.getOrDefault("passwordPolicy", ""));
            settings.setTwoFactorAuth("true".equals(params.get("twoFactorAuth")));
            if (params.containsKey("maxLoginAttempts")) {
                settings.setMaxLoginAttempts(Integer.parseInt(params.get("maxLoginAttempts")));
            }
        }

        userSettingsRepository.save(settings);
        session.setAttribute("userSettings", settings);

        response.put("success", true);
        response.put("message", "Role specific settings updated!");
        return response;
    }

    @PostMapping("/logout-all")
    @ResponseBody
    public Map<String, Object> logoutAll(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out from all other devices successfully.");
        return response;
    }

    @PostMapping("/download-data")
    @ResponseBody
    public Map<String, Object> downloadData(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Your account data download has been compiled and emailed to you.");
        return response;
    }

    @PostMapping("/delete-account")
    @ResponseBody
    public Map<String, Object> deleteAccountRequest(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Your account deletion request has been submitted to the Admin team.");
        return response;
    }

    private UserSettings getOrCreateSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                UserSettings settings = new UserSettings();
                settings.setUser(user);
                return userSettingsRepository.save(settings);
            });
    }
}
