package com.healthcare.system.config;

import com.healthcare.system.entity.Appointment;
import com.healthcare.system.entity.User;
import com.healthcare.system.repository.AppointmentRepository;
import com.healthcare.system.repository.UserRepository;
import com.healthcare.system.service.EmailService;
import com.healthcare.system.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 10000) // Polls every 10 seconds for test responsiveness
    @Transactional
    public void checkAndSendReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Appointment> upcoming = appointmentRepository.findAll();

            for (Appointment appt : upcoming) {
                if ("CANCELLED".equals(appt.getStatus()) || "COMPLETED".equals(appt.getStatus())) {
                    continue;
                }

                LocalDateTime apptTime = appt.getAppointmentDate();
                long minutesDifference = ChronoUnit.MINUTES.between(now, apptTime);

                // Find patient user
                User patientUser = userRepository.findByUsername(appt.getPatient().getEmail()).orElse(null);
                if (patientUser == null) {
                    continue;
                }

                // Check 24-hour reminder: within 24 hours (1440 mins)
                if (minutesDifference > 0 && minutesDifference <= 1440) {
                    boolean sent24h = notificationService.existsReminder(patientUser.getId(), "REMINDER_24H", appt.getId());
                    if (!sent24h) {
                        notificationService.createNotification(patientUser, "PATIENT", "Appointment Reminder (24 Hours)",
                            "Reminder: You have an appointment with Dr. " + appt.getDoctor().getFullName() + " tomorrow at " + apptTime.toLocalTime().toString() + ".",
                            "REMINDER_24H", appt.getId());
                        
                        emailService.sendEmail(patientUser.getUsername(), "Appointment Reminder (24 Hours) - CareGrid",
                            "Dear " + appt.getPatient().getFullName() + ", this is a reminder for your appointment tomorrow with Dr. " + appt.getDoctor().getFullName() + ".");
                    }
                }

                // Check 1-hour reminder: within 1 hour (60 mins)
                if (minutesDifference > 0 && minutesDifference <= 60) {
                    boolean sent1h = notificationService.existsReminder(patientUser.getId(), "REMINDER_1H", appt.getId());
                    if (!sent1h) {
                        notificationService.createNotification(patientUser, "PATIENT", "Appointment Reminder (1 Hour)",
                            "Reminder: Your appointment with Dr. " + appt.getDoctor().getFullName() + " is starting in 1 hour.",
                            "REMINDER_1H", appt.getId());
                        
                        emailService.sendEmail(patientUser.getUsername(), "Appointment Reminder (1 Hour) - CareGrid",
                            "Dear " + appt.getPatient().getFullName() + ", this is a reminder that your appointment with Dr. " + appt.getDoctor().getFullName() + " starts in 1 hour.");
                    }
                }
            }
        } catch (Exception e) {
            // Log scheduler failure but do not crash the app
            System.err.println("ReminderScheduler failed: " + e.getMessage());
        }
    }
}
