package com.healthcare.system.controller;

import com.healthcare.system.entity.*;
import com.healthcare.system.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.healthcare.system.repository.UserRepository;

@Controller
@RequestMapping("/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/add")
    public String showConsultationForm(@RequestParam("appointmentId") Long appointmentId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || (!"ROLE_DOCTOR".equals(user.getRole()) && !"ROLE_DOCTOR".equals(user.getRole()))) {
            // Checked by interceptor, but safe fallback
        }
        
        Appointment appt = appointmentService.getAppointmentById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment ID: " + appointmentId));
        
        // Check if consultation already exists
        if (consultationService.getConsultationByAppointment(appointmentId).isPresent()) {
            return "redirect:/doctor/appointments";
        }

        Consultation consultation = new Consultation();
        consultation.setAppointment(appt);
        consultation.setPatient(appt.getPatient());
        consultation.setDoctor(appt.getDoctor());

        model.addAttribute("appointment", appt);
        model.addAttribute("consultation", consultation);
        return "consultation-form";
    }

    @GetMapping("/add/patient/{patientId}")
    public String showConsultationFormForPatient(@PathVariable("patientId") Long patientId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        
        Patient patient = patientService.getPatientById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID: " + patientId));
        
        Consultation consultation = new Consultation();
        consultation.setPatient(patient);
        consultation.setDoctor(user.getDoctor());

        model.addAttribute("patient", patient);
        model.addAttribute("consultation", consultation);
        return "consultation-form";
    }

    @PostMapping("/save")
    public String saveConsultation(@ModelAttribute("consultation") Consultation consultation,
                                   @RequestParam(value = "appointmentId", required = false) Long appointmentId,
                                   @RequestParam(value = "patientId", required = false) Long patientId,
                                   HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        Doctor doctor = user.getDoctor();

        if (appointmentId != null) {
            Appointment appointment = appointmentService.getAppointmentById(appointmentId).orElseThrow();
            appointment.setStatus("COMPLETED");
            appointmentService.saveAppointment(appointment);

            consultation.setPatient(appointment.getPatient());
            consultation.setDoctor(doctor);
            consultation.setAppointment(appointment);
            consultation.setCreatedDate(LocalDateTime.now());
            consultationService.saveConsultation(consultation);

            // Auto-generate invoice payment record (Pending status, to be managed by Receptionist)
            Payment payment = new Payment();
            payment.setPatient(appointment.getPatient());
            payment.setAppointment(appointment);
            payment.setAmount(doctor.getConsultationFee() != null ? doctor.getConsultationFee() : 150.0);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus("PENDING");
            payment.setPaymentMethod("CARD");
            paymentService.savePayment(payment);
        } else if (patientId != null) {
            Patient patient = patientService.getPatientById(patientId).orElseThrow();
            consultation.setPatient(patient);
            consultation.setDoctor(doctor);
            consultation.setCreatedDate(LocalDateTime.now());
            consultationService.saveConsultation(consultation);
        } else {
            throw new IllegalArgumentException("Either appointmentId or patientId must be provided");
        }

        // Notify patient of updated medical history & consultation notes
        userRepository.findByUsername(consultation.getPatient().getEmail()).ifPresent(patientUser -> {
            notificationService.createNotification(patientUser, "PATIENT", "Medical History Updated", 
                "Your medical history and clinical consultation notes have been updated by Dr. " + doctor.getFullName() + ".", 
                "MEDICAL", consultation.getId());
            
            notificationService.createNotification(patientUser, "PATIENT", "Consultation Notes Uploaded", 
                "Dr. " + doctor.getFullName() + " has uploaded your consultation notes.", 
                "MEDICAL", consultation.getId());
        });

        // Auto-generate a standalone Prescription record if medicineName is entered
        if (consultation.getMedicineName() != null && !consultation.getMedicineName().trim().isEmpty()) {
            Prescription pres = new Prescription();
            pres.setPatient(consultation.getPatient());
            pres.setDoctor(doctor);
            pres.setMedication(consultation.getMedicineName());
            
            String dosageDetails = consultation.getDosage();
            if (consultation.getFrequency() != null && !consultation.getFrequency().isEmpty()) {
                dosageDetails += " (" + consultation.getFrequency() + ")";
            }
            final String finalDosageDetails = dosageDetails;
            pres.setDosage(dosageDetails);
            pres.setDuration(consultation.getDuration());
            pres.setInstructions(consultation.getAdditionalInstructions());
            pres.setPrescribedDate(LocalDate.now());
            prescriptionService.savePrescription(pres);

            // Notify patient of new prescription
            userRepository.findByUsername(consultation.getPatient().getEmail()).ifPresent(patientUser -> {
                notificationService.createNotification(patientUser, "PATIENT", "New Prescription Added", 
                    "A new prescription for " + consultation.getMedicineName() + " has been added by Dr. " + doctor.getFullName() + ".", 
                    "PRESCRIPTION", pres.getId());
                emailService.sendNewPrescriptionNotification(patientUser.getUsername(), consultation.getPatient().getFullName(), 
                    doctor.getFullName(), consultation.getMedicineName(), finalDosageDetails);
            });
        }

        return "redirect:/doctor/patients/records/" + consultation.getPatient().getId();
    }

    @GetMapping("/edit/{id}")
    public String showEditConsultationForm(@PathVariable("id") Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        
        Consultation consultation = consultationService.getConsultationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid consultation ID: " + id));

        model.addAttribute("consultation", consultation);
        if (consultation.getAppointment() != null) {
            model.addAttribute("appointment", consultation.getAppointment());
        } else {
            model.addAttribute("patient", consultation.getPatient());
        }
        return "consultation-edit-form";
    }

    @PostMapping("/update")
    public String updateConsultation(@ModelAttribute("consultation") Consultation consultation, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        
        Consultation existing = consultationService.getConsultationById(consultation.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid consultation ID: " + consultation.getId()));
        
        // Basic Info
        existing.setHeight(consultation.getHeight());
        existing.setWeight(consultation.getWeight());
        existing.setBloodGroup(consultation.getBloodGroup());
        existing.setBloodPressure(consultation.getBloodPressure());
        
        // Medical History
        existing.setChiefComplaint(consultation.getChiefComplaint());
        existing.setCurrentSymptoms(consultation.getCurrentSymptoms());
        existing.setPreviousDiseases(consultation.getPreviousDiseases());
        existing.setAllergies(consultation.getAllergies());
        existing.setCurrentMedications(consultation.getCurrentMedications());
        existing.setPreviousMedications(consultation.getPreviousMedications());
        existing.setSurgeries(consultation.getSurgeries());
        existing.setFamilyMedicalHistory(consultation.getFamilyMedicalHistory());
        existing.setLifestyleInfo(consultation.getLifestyleInfo());
        
        // Notes & Assessment
        existing.setDiagnosis(consultation.getDiagnosis());
        existing.setTreatmentPlan(consultation.getTreatmentPlan());
        existing.setNotes(consultation.getNotes());
        existing.setFollowUpInstructions(consultation.getFollowUpInstructions());
        
        // Prescription
        existing.setMedicineName(consultation.getMedicineName());
        existing.setDosage(consultation.getDosage());
        existing.setFrequency(consultation.getFrequency());
        existing.setDuration(consultation.getDuration());
        existing.setAdditionalInstructions(consultation.getAdditionalInstructions());
        
        consultationService.saveConsultation(existing);

        // Notify patient of updated medical history & prescriptions
        userRepository.findByUsername(existing.getPatient().getEmail()).ifPresent(patientUser -> {
            notificationService.createNotification(patientUser, "PATIENT", "Medical History Updated", 
                "Your medical history and clinical consultation notes have been updated by Dr. " + user.getDoctor().getFullName() + ".", 
                "MEDICAL", existing.getId());
            
            if (existing.getMedicineName() != null && !existing.getMedicineName().trim().isEmpty()) {
                notificationService.createNotification(patientUser, "PATIENT", "Prescription Updated", 
                    "Your prescription for " + existing.getMedicineName() + " has been updated by Dr. " + user.getDoctor().getFullName() + ".", 
                    "PRESCRIPTION", existing.getId());
                emailService.sendNewPrescriptionNotification(patientUser.getUsername(), existing.getPatient().getFullName(), 
                    user.getDoctor().getFullName(), existing.getMedicineName(), existing.getDosage());
            }
        });

        return "redirect:/doctor/patients/records/" + existing.getPatient().getId();
    }
}
