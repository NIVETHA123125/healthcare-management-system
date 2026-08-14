package com.healthcare.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:mynew222028@gmail.com}")
    private String mailFrom;

    public void sendEmail(String to, String subject, String contentHtml) {
        // Output a beautiful console log
        System.out.println("=========================================================================");
        System.out.println("📬 SIMULATED EMAIL DISPATCH");
        System.out.println("To:      " + to);
        System.out.println("Subject: " + subject);
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(contentHtml.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
        System.out.println("=========================================================================");

        if (mailSender == null) {
            logger.info("JavaMailSender not configured. Email logged to console.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(contentHtml, true);
            helper.setFrom(mailFrom != null && !mailFrom.isEmpty() ? mailFrom : "mynew222028@gmail.com");
            mailSender.send(message);
            logger.info("Email sent successfully to " + to);
        } catch (Exception e) {
            logger.warning("Failed to send real email to " + to + " via SMTP: " + e.getMessage() + ". Logged to console instead.");
            e.printStackTrace();
            throw new RuntimeException("SMTP email dispatch failed: " + e.getMessage(), e);
        }
    }

    public void sendAppointmentConfirmation(String to, String patientName, String doctorName, String dateStr, String timeStr) {
        String html = "<html><body>"
                + "<h2>Appointment Booking Confirmation</h2>"
                + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                + "<p>Your appointment has been successfully scheduled with <strong>Dr. " + doctorName + "</strong>.</p>"
                + "<p><strong>Date:</strong> " + dateStr + "<br>"
                + "<strong>Time:</strong> " + timeStr + "</p>"
                + "<p>Thank you for choosing CareGrid Healthcare.</p>"
                + "</body></html>";
        sendEmail(to, "Appointment Confirmed - CareGrid", html);
    }

    public void sendAppointmentCancellation(String to, String patientName, String doctorName, String dateTimeStr) {
        String html = "<html><body>"
                + "<h2>Appointment Cancellation Notice</h2>"
                + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                + "<p>Please note that your scheduled appointment with <strong>Dr. " + doctorName + "</strong> on <strong>" + dateTimeStr + "</strong> has been cancelled.</p>"
                + "<p>If this was unexpected, please log into the portal to reschedule or contact support.</p>"
                + "</body></html>";
        sendEmail(to, "Appointment Cancelled - CareGrid", html);
    }

    public void sendAppointmentRescheduled(String to, String patientName, String doctorName, String oldDateTimeStr, String newDateTimeStr) {
        String html = "<html><body>"
                + "<h2>Appointment Rescheduled</h2>"
                + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                + "<p>Your appointment with <strong>Dr. " + doctorName + "</strong> has been rescheduled.</p>"
                + "<p><strong>Previous Time:</strong> " + oldDateTimeStr + "<br>"
                + "<strong>New Time:</strong> " + newDateTimeStr + "</p>"
                + "<p>We look forward to seeing you.</p>"
                + "</body></html>";
        sendEmail(to, "Appointment Rescheduled - CareGrid", html);
    }

    public void sendPasswordResetConfirmation(String to, String fullName) {
        String html = "<html><body>"
                + "<h2>Password Reset Successful</h2>"
                + "<p>Dear <strong>" + fullName + "</strong>,</p>"
                + "<p>Your CareGrid portal account password was successfully reset.</p>"
                + "<p>If you did not request this change, please contact administration immediately.</p>"
                + "</body></html>";
        sendEmail(to, "Password Reset Success - CareGrid", html);
    }

    public void sendNewPrescriptionNotification(String to, String patientName, String doctorName, String medication, String dosage) {
        String html = "<html><body>"
                + "<h2>New Prescription Added</h2>"
                + "<p>Dear <strong>" + patientName + "</strong>,</p>"
                + "<p><strong>Dr. " + doctorName + "</strong> has prescribed new medication for you:</p>"
                + "<p><strong>Medication:</strong> " + medication + "<br>"
                + "<strong>Dosage:</strong> " + dosage + "</p>"
                + "<p>Please log into your Patient Portal to view complete instructions and download your official Rx prescription pad.</p>"
                + "</body></html>";
        sendEmail(to, "New Prescription Issued - CareGrid", html);
    }

    public void sendOtpEmail(String to, String otp) {
        String html = "<html><body>"
                + "<p>Hello,</p>"
                + "<p>Your One-Time Password (OTP) is:</p>"
                + "<h3 style='font-size: 24px; font-family: monospace; letter-spacing: 2px;'>" + otp + "</h3>"
                + "<p>This OTP is valid for 5 minutes.</p>"
                + "<p>If you did not request this verification, please ignore this email.</p>"
                + "<p>Thank you,<br>Healthcare Management System Team</p>"
                + "</body></html>";
        sendEmail(to, "Healthcare Management System - Email Verification", html);
    }
}
