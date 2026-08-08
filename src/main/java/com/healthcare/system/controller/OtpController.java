package com.healthcare.system.controller;

import com.healthcare.system.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.healthcare.system.service.UserService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestParam String email) {
        OtpService.OtpResponse response = otpService.generateAndSendOtp(email);
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        result.put("message", response.getMessage());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/send-for-recovery")
    public ResponseEntity<Map<String, Object>> sendOtpForRecovery(@RequestParam String email, @RequestParam String dob) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate parsedDob = LocalDate.parse(dob);
            if (userService.verifyForgotPassword(email, parsedDob)) {
                OtpService.OtpResponse response = otpService.generateAndSendOtp(email);
                result.put("success", response.isSuccess());
                result.put("message", response.getMessage());
            } else {
                result.put("success", false);
                result.put("message", "Verification failed. Incorrect email or Date of Birth.");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Invalid date format.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        OtpService.OtpResponse response = otpService.verifyOtp(email, otp);
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        result.put("message", response.getMessage());
        return ResponseEntity.ok(result);
    }
}
