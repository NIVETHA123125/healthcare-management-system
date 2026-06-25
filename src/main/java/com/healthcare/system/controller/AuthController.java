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
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage(HttpSession session,
                            @RequestParam(value = "registrationSuccess", required = false) String regSuccess,
                            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                            Model model) {
        if (session.getAttribute("loggedInUser") != null) {
            User user = (User) session.getAttribute("loggedInUser");
            if ("ROLE_PATIENT".equals(user.getRole())) {
                return "redirect:/patient/dashboard";
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

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Optional<User> authenticatedUser = userService.authenticate(username, password);
        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            session.setAttribute("loggedInUser", user);
            if ("ROLE_PATIENT".equals(user.getRole())) {
                return "redirect:/patient/dashboard";
            }
            return "redirect:/dashboard";
        } else {
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

    @PostMapping("/reset-password")
    public String handlePasswordReset(@RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }
        boolean success = userService.resetPassword(email, password);
        if (success) {
            return "redirect:/login?resetSuccess=true";
        } else {
            model.addAttribute("error", "User not found or password update failed.");
            return "forgot-password";
        }
    }
}
