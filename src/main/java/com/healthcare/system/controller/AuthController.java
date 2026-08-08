package com.healthcare.system.controller;

import com.healthcare.system.entity.User;
import com.healthcare.system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.healthcare.system.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;

import com.healthcare.system.service.NotificationService;
import com.healthcare.system.service.EmailService;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String loginPage(HttpSession session,
                            @RequestParam(value = "registrationSuccess", required = false) String regSuccess,
                            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                            Model model) {
        if (session.getAttribute("loggedInUser") != null) {
            User user = (User) session.getAttribute("loggedInUser");
            if ("ROLE_PATIENT".equals(user.getRole()) || "PATIENT".equals(user.getRole())) {
                return "redirect:/patient/dashboard";
            }
            if ("ROLE_DOCTOR".equals(user.getRole()) || "DOCTOR".equals(user.getRole())) {
                return "redirect:/doctor/dashboard";
            }
            return "redirect:/dashboard";
        }
        if ("true".equals(regSuccess)) {
            model.addAttribute("successMessage", "Registration successful! You can now log in.");
        }
        if ("true".equals(resetSuccess)) {
            model.addAttribute("successMessage", "Password reset successful! Please log in with your new password.");
        }
        return "login";
    }

    private final java.util.concurrent.ConcurrentHashMap<String, Integer> failedLoginAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam String role,
                        HttpSession session,
                        Model model) {
        String normUsername = username.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(normUsername);
        
        if (userOpt.isEmpty()) {
            if ("DOCTOR".equals(role)) {
                model.addAttribute("error", "Doctor account not found. Please contact the administrator.");
            } else if ("ADMIN".equals(role)) {
                model.addAttribute("error", "Admin account not found.");
            } else {
                model.addAttribute("error", "Invalid username or password");
            }
            return "login";
        }
        
        User user = userOpt.get();
        boolean isPatient = "ROLE_PATIENT".equals(user.getRole()) || "PATIENT".equals(user.getRole());
        boolean isDoctor = "ROLE_DOCTOR".equals(user.getRole()) || "DOCTOR".equals(user.getRole());
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole());
        
        boolean roleMatches = false;
        if ("PATIENT".equals(role) && isPatient) {
            roleMatches = true;
        } else if ("DOCTOR".equals(role) && isDoctor) {
            roleMatches = true;
        } else if ("ADMIN".equals(role) && isAdmin) {
            roleMatches = true;
        }
        
        if (!roleMatches) {
            if ("DOCTOR".equals(role)) {
                model.addAttribute("error", "Doctor account not found. Please contact the administrator.");
            } else if ("ADMIN".equals(role)) {
                model.addAttribute("error", "Admin account not found.");
            } else {
                model.addAttribute("error", "Invalid username or password");
            }
            return "login";
        }
        
        // Verify password
        Optional<User> authenticatedUser = userService.authenticate(username, password);
        if (authenticatedUser.isPresent()) {
            failedLoginAttempts.remove(normUsername);
            session.setAttribute("loggedInUser", user);
            if (isPatient) {
                return "redirect:/patient/dashboard";
            } else if (isDoctor) {
                return "redirect:/doctor/dashboard";
            }
            return "redirect:/dashboard";
        } else {
            int attempts = failedLoginAttempts.getOrDefault(normUsername, 0) + 1;
            failedLoginAttempts.put(normUsername, attempts);
            if (attempts >= 3) {
                notificationService.createNotification(null, "ADMIN", "Multiple Failed Logins", 
                    "Multiple failed login attempts detected for account: " + username, "SYSTEM", null);
            }
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }


    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String verifyForgotPassword(@RequestParam String email,
                                       @RequestParam String dob,
                                       Model model) {
        try {
            LocalDate parsedDob = LocalDate.parse(dob);
            if (userService.verifyForgotPassword(email, parsedDob)) {
                model.addAttribute("email", email);
                model.addAttribute("dob", dob); // Pass DOB for subsequent verification
                return "reset-password";
            } else {
                model.addAttribute("error", "Verification failed. Incorrect email or Date of Birth.");
                return "forgot-password";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Invalid date format. Use YYYY-MM-DD.");
            return "forgot-password";
        }
    }

    @Autowired
    private com.healthcare.system.service.OtpService otpService;

    @PostMapping("/reset-password")
    public String handlePasswordReset(@RequestParam String email,
                                      @RequestParam String dob,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        
        // Backend OTP verification verification
        boolean isVerified = false;
        try {
            isVerified = otpService.markOtpAsUsed(email);
        } catch (Exception e) {
            isVerified = false;
        }

        if (!isVerified) {
            model.addAttribute("email", email);
            model.addAttribute("dob", dob);
            model.addAttribute("error", "Email verification failed or expired. Please verify with OTP again.");
            return "forgot-password";
        }

        try {
            LocalDate parsedDob = LocalDate.parse(dob);
            if (!userService.verifyForgotPassword(email, parsedDob)) {
                model.addAttribute("email", email);
                model.addAttribute("dob", dob);
                model.addAttribute("error", "Invalid Date of Birth. Password reset failed.");
                return "reset-password";
            }
        } catch (Exception e) {
            model.addAttribute("email", email);
            model.addAttribute("dob", dob);
            model.addAttribute("error", "Invalid Date of Birth. Password reset failed.");
            return "reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("dob", dob);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }
        boolean success = userService.resetPassword(email, password);
        if (success) {
            // Notify user of successful password reset
            userRepository.findByUsername(email.trim().toLowerCase()).ifPresent(resetUser -> {
                String normalizedRole = resetUser.getRole();
                if (normalizedRole.startsWith("ROLE_")) {
                    normalizedRole = normalizedRole.replace("ROLE_", "");
                }
                notificationService.createNotification(resetUser, normalizedRole, "Password Reset Successful", 
                    "Your portal password has been successfully reset.", "ACCOUNT", resetUser.getId());
                emailService.sendPasswordResetConfirmation(resetUser.getUsername(), resetUser.getFullName());
            });
            return "redirect:/login?resetSuccess=true";
        } else {
            model.addAttribute("error", "User not found or password update failed.");
            return "forgot-password";
        }
    }
}
