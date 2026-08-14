package com.healthcare.system.service;

import com.healthcare.system.entity.OtpEntity;
import com.healthcare.system.repository.OtpRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 3;

    @Transactional
    public OtpResponse generateAndSendOtp(String email) {
        Optional<OtpEntity> latestOtpOpt = otpRepository.findTopByEmailOrderByCreatedTimeDesc(email);
        
        if (latestOtpOpt.isPresent()) {
            OtpEntity latestOtp = latestOtpOpt.get();
            if (LocalDateTime.now().isBefore(latestOtp.getCreatedTime().plusSeconds(COOLDOWN_SECONDS))) {
                return new OtpResponse(false, "Please wait before requesting a new OTP.");
            }
        }

        String rawOtp = generateRandomOtp();
        String hashedOtp = BCrypt.hashpw(rawOtp, BCrypt.gensalt());

        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setEmail(email);
        otpEntity.setOtp(hashedOtp);
        otpEntity.setAttempts(0);
        otpEntity.setVerified(false);
        otpEntity.setUsed(false);
        otpEntity.setCreatedTime(LocalDateTime.now());
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));

        otpRepository.save(otpEntity);
                
        try {
            emailService.sendOtpEmail(email, rawOtp);
            return new OtpResponse(true, "OTP sent successfully to " + email);
        } catch (Exception e) {
            return new OtpResponse(false, "Failed to send email via SMTP: " + e.getMessage());
        }
    }

    @Transactional
    public OtpResponse verifyOtp(String email, String rawOtp) {
        Optional<OtpEntity> latestOtpOpt = otpRepository.findTopByEmailOrderByCreatedTimeDesc(email);
        
        if (latestOtpOpt.isEmpty()) {
            return new OtpResponse(false, "No OTP found for this email.");
        }

        OtpEntity otpEntity = latestOtpOpt.get();

        if (otpEntity.isUsed()) {
            return new OtpResponse(false, "OTP has already been used.");
        }

        if (LocalDateTime.now().isAfter(otpEntity.getExpiryTime())) {
            return new OtpResponse(false, "OTP has expired.");
        }

        if (otpEntity.getAttempts() >= MAX_ATTEMPTS) {
            return new OtpResponse(false, "Maximum verification attempts reached. Please request a new OTP.");
        }

        otpEntity.setAttempts(otpEntity.getAttempts() + 1);

        if (BCrypt.checkpw(rawOtp, otpEntity.getOtp())) {
            otpEntity.setVerified(true);
            otpRepository.save(otpEntity);
            return new OtpResponse(true, "OTP verified successfully.");
        } else {
            otpRepository.save(otpEntity);
            return new OtpResponse(false, "Invalid OTP.");
        }
    }

    @Transactional
    public boolean markOtpAsUsed(String email) {
        Optional<OtpEntity> latestOtpOpt = otpRepository.findTopByEmailOrderByCreatedTimeDesc(email);
        if (latestOtpOpt.isPresent()) {
            OtpEntity otpEntity = latestOtpOpt.get();
            if (otpEntity.isVerified() && !otpEntity.isUsed()) {
                otpEntity.setUsed(true);
                otpRepository.save(otpEntity);
                return true;
            }
        }
        return false;
    }

    private String generateRandomOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    public static class OtpResponse {
        private boolean success;
        private String message;

        public OtpResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
