package com.healthcare.system.controller;

import com.healthcare.system.entity.Appointment;
import com.healthcare.system.entity.Doctor;
import com.healthcare.system.entity.Patient;
import com.healthcare.system.service.AppointmentService;
import com.healthcare.system.service.DoctorService;
import com.healthcare.system.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

import com.healthcare.system.service.NotificationService;
import com.healthcare.system.service.EmailService;
import com.healthcare.system.repository.UserRepository;
import com.healthcare.system.repository.AppointmentRepository;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping
    public String listAppointments(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "todayOnly", required = false) Boolean todayOnly,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            Model model) {
        
        java.time.LocalDateTime startDate = null;
        java.time.LocalDateTime endDate = null;
        
        if (Boolean.TRUE.equals(todayOnly)) {
            startDate = java.time.LocalDate.now().atStartOfDay();
            endDate = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        } else if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                java.time.LocalDate parsed = java.time.LocalDate.parse(dateStr);
                startDate = parsed.atStartOfDay();
                endDate = parsed.atTime(java.time.LocalTime.MAX);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        
        List<Appointment> appointments = appointmentRepository.searchAndFilterAppointments(
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            status != null && !status.trim().isEmpty() ? status.trim() : null,
            doctorId,
            startDate,
            endDate
        );
        
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("date", dateStr);
        model.addAttribute("todayOnly", todayOnly);
        model.addAttribute("selectedDoctorId", doctorId);
        return "appointments";
    }

    @GetMapping("/book")
    public String showBookForm(Model model) {
        List<Patient> patients = patientService.getAllPatients();
        List<Doctor> doctors = doctorService.getAllDoctors();
        model.addAttribute("patients", patients);
        model.addAttribute("doctors", doctors);
        model.addAttribute("appointment", new Appointment());
        return "appointment-form";
    }

    @PostMapping("/save")
    public String saveAppointment(@RequestParam("patientId") Long patientId,
                                  @RequestParam("doctorId") Long doctorId,
                                  @RequestParam("appointmentDate") String dateStr,
                                  @RequestParam("appointmentTime") String timeStr,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment date cannot be empty.");
                return "redirect:/appointments/book";
            }
            if (timeStr == null || timeStr.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment time cannot be empty.");
                return "redirect:/appointments/book";
            }

            Patient patient = patientService.getPatientById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid patient Id: " + patientId));
            Doctor doctor = doctorService.getDoctorById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid doctor Id: " + doctorId));
            
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime appointmentDateTime = LocalDateTime.of(date, time);

            if (date.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot book an appointment for a past date.");
                return "redirect:/appointments/book";
            }

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setAppointmentDate(appointmentDateTime);
            appointment.setNotes(notes);
            appointment.setStatus("APPROVED");
            
            appointmentService.saveAppointment(appointment);

            // Patient notification
            userRepository.findByUsername(patient.getEmail()).ifPresent(patientUser -> {
                notificationService.createNotification(patientUser, "PATIENT", "Appointment Scheduled", 
                    "An appointment has been scheduled for you with Dr. " + doctor.getFullName() + " for " + appointmentDateTime.toString() + ".", 
                    "APPOINTMENT", appointment.getId());
                emailService.sendAppointmentConfirmation(patientUser.getUsername(), patient.getFullName(), doctor.getFullName(), 
                    dateStr, timeStr);
            });

            // Doctor notification
            userRepository.findByUsername(doctor.getEmail()).ifPresent(doctorUser -> {
                notificationService.createNotification(doctorUser, "DOCTOR", "New Appointment Scheduled", 
                    "A new appointment has been scheduled for patient " + patient.getFullName() + " for " + appointmentDateTime.toString() + ".", 
                    "APPOINTMENT", appointment.getId());
            });

            redirectAttributes.addFlashAttribute("successMessage", "Appointment scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to book appointment: " + e.getMessage());
        }
        return "redirect:/appointments";
    }

    @GetMapping("/approve/{id}")
    public String approveAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));
        appointment.setStatus("APPROVED");
        appointmentService.saveAppointment(appointment);

        // Patient notification
        userRepository.findByUsername(appointment.getPatient().getEmail()).ifPresent(patientUser -> {
            notificationService.createNotification(patientUser, "PATIENT", "Appointment Approved", 
                "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " on " + appointment.getAppointmentDate().toString() + " has been approved.", 
                "APPOINTMENT", appointment.getId());
            emailService.sendAppointmentConfirmation(patientUser.getUsername(), appointment.getPatient().getFullName(), appointment.getDoctor().getFullName(), 
                appointment.getAppointmentDate().toLocalDate().toString(), appointment.getAppointmentDate().toLocalTime().toString());
        });

        return "redirect:/appointments";
    }

    @GetMapping("/complete/{id}")
    public String completeAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));
        appointment.setStatus("COMPLETED");
        appointmentService.saveAppointment(appointment);
        return "redirect:/appointments";
    }

    @GetMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));
        appointment.setStatus("CANCELLED");
        appointmentService.saveAppointment(appointment);

        // Patient notification
        userRepository.findByUsername(appointment.getPatient().getEmail()).ifPresent(patientUser -> {
            notificationService.createNotification(patientUser, "PATIENT", "Appointment Cancelled", 
                "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " on " + appointment.getAppointmentDate().toString() + " has been cancelled.", 
                "APPOINTMENT", appointment.getId());
            emailService.sendAppointmentCancellation(patientUser.getUsername(), appointment.getPatient().getFullName(), appointment.getDoctor().getFullName(), appointment.getAppointmentDate().toString());
        });

        // Admin notification
        notificationService.createNotification(null, "ADMIN", "Appointment Cancelled", 
            "Appointment (ID: " + appointment.getId() + ") was cancelled by Staff.", "APPOINTMENT", appointment.getId());

        return "redirect:/appointments";
    }

    @GetMapping("/history")
    public String appointmentHistory(Model model) {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        model.addAttribute("appointments", appointments);
        return "history";
    }
}
