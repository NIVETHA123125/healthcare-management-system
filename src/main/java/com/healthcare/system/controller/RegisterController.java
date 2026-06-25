package com.healthcare.system.controller;

import com.healthcare.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String registerPage(Model model) {
        return "register";
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
        
        // 1. Password mismatch validation
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        // 2. Email uniqueness validation
        if (userService.existsByUsername(email)) {
            model.addAttribute("error", "Email is already registered. Please login or try another email.");
            return "register";
        }

        try {
            LocalDate parsedDob = LocalDate.parse(dob);
            userService.registerPatient(fullName, email, phone, gender, parsedDob, address, password);
            return "redirect:/login?registrationSuccess=true";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to register: " + e.getMessage());
            return "register";
        }
    }
}
