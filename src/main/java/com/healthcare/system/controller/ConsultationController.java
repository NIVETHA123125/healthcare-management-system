package com.healthcare.system.controller;

import com.healthcare.system.entity.Appointment;
import com.healthcare.system.entity.Consultation;
import com.healthcare.system.entity.Payment;
import com.healthcare.system.service.AppointmentService;
import com.healthcare.system.service.ConsultationService;
import com.healthcare.system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/add")
    public String showConsultationForm(@RequestParam("appointmentId") Long appointmentId, Model model) {
        Appointment appt = appointmentService.getAppointmentById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment ID: " + appointmentId));
        
        // Check if consultation already exists
        if (consultationService.getConsultationByAppointment(appointmentId).isPresent()) {
            return "redirect:/appointments";
        }

        model.addAttribute("appointment", appt);
        return "consultation-form";
    }

    @PostMapping("/save")
    public String saveConsultation(@RequestParam("appointmentId") Long appointmentId,
                                   @RequestParam("diagnosis") String diagnosis,
                                   @RequestParam("prescription") String prescription,
                                   @RequestParam("notes") String notes) {
        
        Appointment appointment = appointmentService.getAppointmentById(appointmentId).orElseThrow();
        appointment.setStatus("COMPLETED");
        appointmentService.saveAppointment(appointment);

        Consultation consultation = new Consultation();
        consultation.setPatient(appointment.getPatient());
        consultation.setDoctor(appointment.getDoctor());
        consultation.setAppointment(appointment);
        consultation.setDiagnosis(diagnosis);
        consultation.setPrescription(prescription);
        consultation.setNotes(notes);
        consultation.setCreatedDate(LocalDateTime.now());
        consultationService.saveConsultation(consultation);

        // Auto-generate invoice payment record (Pending status, to be managed by Receptionist)
        Payment payment = new Payment();
        payment.setPatient(appointment.getPatient());
        payment.setAppointment(appointment);
        payment.setAmount(150.0); // Standard consultation fee
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("PENDING");
        payment.setPaymentMethod("CARD");
        paymentService.savePayment(payment);

        return "redirect:/appointments";
    }
}
