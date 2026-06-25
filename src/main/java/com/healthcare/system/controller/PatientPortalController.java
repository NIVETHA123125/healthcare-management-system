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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
public class PatientPortalController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        Patient patient = user.getPatient();

        // 1. Get upcoming appointments for this patient
        List<Appointment> allAppts = appointmentService.getAppointmentsByPatient(patient.getId());
        List<Appointment> upcomingAppts = allAppts.stream()
                .filter(a -> a.getAppointmentDate().isAfter(LocalDateTime.now()) && "SCHEDULED".equals(a.getStatus()))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("upcomingAppointments", upcomingAppts);

        // 2. Get recent consultations
        List<Consultation> consultations = consultationService.getConsultationsByPatient(patient.getId());
        List<Consultation> recentConsultations = consultations.stream()
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("recentConsultations", recentConsultations);

        // 3. Get payments summary
        List<Payment> payments = paymentService.getPaymentsByPatient(patient.getId());
        double totalPaid = payments.stream().filter(p -> "PAID".equals(p.getStatus())).mapToDouble(Payment::getAmount).sum();
        double totalPending = payments.stream().filter(p -> "PENDING".equals(p.getStatus())).mapToDouble(Payment::getAmount).sum();
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("recentPayments", payments.stream().limit(3).collect(Collectors.toList()));

        // 4. Sample announcements
        model.addAttribute("announcements", List.of(
            "COVID-19 vaccination booster shots are now available on Wednesdays and Fridays.",
            "Dr. Shankar R will be unavailable on July 4th due to national holiday schedule changes.",
            "CareGrid portal now supports online self-service billing invoice viewing."
        ));

        return "patient-dashboard";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, @RequestParam(value = "success", required = false) String success, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("patient", user.getPatient());
        if ("true".equals(success)) {
            model.addAttribute("successMessage", "Profile updated successfully!");
        }
        return "patient-profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String phone,
                                @RequestParam String gender,
                                @RequestParam String dob,
                                @RequestParam String address,
                                HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Patient patient = user.getPatient();

        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setPhone(phone);
        patient.setGender(gender);
        patient.setDob(LocalDate.parse(dob));
        patient.setAddress(address);
        patientService.savePatient(patient);

        user.setFullName(firstName + " " + lastName);
        userService.save(user);

        return "redirect:/patient/profile?success=true";
    }

    @GetMapping("/appointments")
    public String appointmentsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(user.getPatient().getId());
        model.addAttribute("appointments", appointments);
        return "patient-appointments";
    }

    @GetMapping("/appointments/book")
    public String showBookForm(Model model) {
        List<Doctor> doctors = doctorService.getAllDoctors();
        model.addAttribute("doctors", doctors);
        return "patient-book-form";
    }

    @PostMapping("/appointments/save")
    public String saveAppointment(@RequestParam Long doctorId,
                                  @RequestParam String appointmentDate,
                                  @RequestParam(required = false) String notes,
                                  HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Doctor doctor = doctorService.getDoctorById(doctorId).orElseThrow();

        Appointment appointment = new Appointment();
        appointment.setPatient(user.getPatient());
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(LocalDateTime.parse(appointmentDate));
        appointment.setNotes(notes);
        appointment.setStatus("SCHEDULED");

        appointmentService.saveAppointment(appointment);
        return "redirect:/patient/appointments";
    }

    @GetMapping("/medical-records")
    public String medicalRecordsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        List<Consultation> consultations = consultationService.getConsultationsByPatient(user.getPatient().getId());
        model.addAttribute("consultations", consultations);
        return "patient-medical-records";
    }

    @GetMapping("/payments")
    public String paymentsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        List<Payment> payments = paymentService.getPaymentsByPatient(user.getPatient().getId());
        model.addAttribute("payments", payments);
        return "patient-payments";
    }

    @GetMapping("/payments/receipt/{id}")
    public String viewReceipt(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        Payment payment = paymentService.getPaymentById(id).orElseThrow();

        // Enforce that patients can only view their own receipts
        if (!payment.getPatient().getId().equals(user.getPatient().getId())) {
            return "redirect:/patient/payments";
        }

        model.addAttribute("payment", payment);
        return "patient-receipt";
    }
}
