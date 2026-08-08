package com.healthcare.system.controller;

import com.healthcare.system.entity.*;
import com.healthcare.system.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.healthcare.system.repository.UserRepository;
import com.healthcare.system.repository.AppointmentRepository;
import com.healthcare.system.repository.DoctorRepository;
import com.healthcare.system.service.NotificationService;
import com.healthcare.system.service.EmailService;

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

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        Patient patient = user.getPatient();

        // 1. Get upcoming appointments for this patient
        List<Appointment> allAppts = appointmentService.getAppointmentsByPatient(patient.getId());
        List<Appointment> upcomingAppts = allAppts.stream()
                .filter(a -> a.getAppointmentDate().isAfter(LocalDateTime.now()) && ("PENDING".equals(a.getStatus()) || "APPROVED".equals(a.getStatus())))
                .sorted((a1, a2) -> a1.getAppointmentDate().compareTo(a2.getAppointmentDate()))
                .collect(Collectors.toList());
        model.addAttribute("upcomingAppointments", upcomingAppts.stream().limit(5).collect(Collectors.toList()));
        
        long upcomingApptsCount = upcomingAppts.size();
        model.addAttribute("upcomingApptsCount", upcomingApptsCount);
        model.addAttribute("totalApptsCount", allAppts.size());

        Appointment nextAppointment = upcomingAppts.isEmpty() ? null : upcomingAppts.get(0);
        model.addAttribute("nextAppointment", nextAppointment);

        // 2. Get recent consultations
        List<Consultation> consultations = consultationService.getConsultationsByPatient(patient.getId());
        List<Consultation> recentConsultations = consultations.stream()
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("recentConsultations", recentConsultations);
        model.addAttribute("medicalRecordsCount", consultations.size());

        // 3. Get payments summary
        List<Payment> payments = paymentService.getPaymentsByPatient(patient.getId());
        double totalPaid = payments.stream().filter(p -> "PAID".equals(p.getStatus())).mapToDouble(Payment::getAmount).sum();
        double totalPending = payments.stream().filter(p -> "PENDING".equals(p.getStatus()) || "FAILED".equals(p.getStatus())).mapToDouble(Payment::getAmount).sum();
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("recentPayments", payments.stream().limit(3).collect(Collectors.toList()));
        
        Optional<Payment> lastPayment = payments.stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .max((p1, p2) -> p1.getPaymentDate().compareTo(p2.getPaymentDate()));
        model.addAttribute("lastPaymentDate", lastPayment.map(Payment::getPaymentDate).orElse(null));

        // 4. Sample announcements
        model.addAttribute("announcements", List.of(
            "COVID-19 vaccination booster shots are now available on Wednesdays and Fridays.",
            "Dr. Shankar R will be unavailable on July 4th due to national holiday schedule changes.",
            "CareGrid portal now supports online self-service billing invoice viewing."
        ));

        // 5. Prescriptions
        List<Prescription> patientPrescriptions = prescriptionService.getPrescriptionsByPatientId(patient.getId());
        model.addAttribute("totalPrescriptionsCount", patientPrescriptions.size());
        model.addAttribute("recentPrescriptions", patientPrescriptions.stream().limit(3).collect(Collectors.toList()));

        // 6. Last Appointment Date
        Optional<Appointment> lastCompletedAppt = allAppts.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()) || (a.getAppointmentDate().isBefore(LocalDateTime.now()) && "APPROVED".equals(a.getStatus())))
                .max((a1, a2) -> a1.getAppointmentDate().compareTo(a2.getAppointmentDate()));
        model.addAttribute("lastAppointmentDate", lastCompletedAppt.map(a -> a.getAppointmentDate().toLocalDate()).orElse(null));

        // 7. Notifications
        long unreadNotificationCount = notificationService.countUnread(user, "PATIENT");
        model.addAttribute("unreadNotificationCount", unreadNotificationCount);
        model.addAttribute("recentNotifications", notificationService.getHistory(user, "PATIENT").stream().limit(5).collect(Collectors.toList()));

        // 8. Welcome Greeting
        int hour = LocalTime.now().getHour();
        String greetingPrefix = "Good Evening";
        if (hour >= 5 && hour < 12) {
            greetingPrefix = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greetingPrefix = "Good Afternoon";
        }
        model.addAttribute("greeting", greetingPrefix + ", " + patient.getFullName() + " 👋");

        // 9. Health Summary
        String bloodGroup = "O+";
        String allergies = "No known allergies";
        String emergencyContact = "N/A";
        String chronicConditions = "None";
        String primaryDoctorName = "No primary doctor assigned.";

        String history = patient.getMedicalHistory();
        if (history != null && !history.trim().isEmpty()) {
            if (history.toLowerCase().contains("blood")) {
                Pattern p = Pattern.compile("(?i)blood\\s*(?:group)?\\s*:\\s*([^,\\n]+)");
                Matcher m = p.matcher(history);
                if (m.find()) bloodGroup = m.group(1).trim();
            }
            if (history.toLowerCase().contains("allerg")) {
                Pattern p = Pattern.compile("(?i)allerg(?:y|ies)?\\s*:\\s*([^,\\n]+)");
                Matcher m = p.matcher(history);
                if (m.find()) allergies = m.group(1).trim();
            }
            if (history.toLowerCase().contains("emergency")) {
                Pattern p = Pattern.compile("(?i)emergency\\s*(?:contact)?\\s*:\\s*([^,\\n]+)");
                Matcher m = p.matcher(history);
                if (m.find()) emergencyContact = m.group(1).trim();
            }
            if (history.toLowerCase().contains("chronic") || history.toLowerCase().contains("condition")) {
                Pattern p = Pattern.compile("(?i)(?:chronic|condition)\\s*:\\s*([^,\\n]+)");
                Matcher m = p.matcher(history);
                if (m.find()) chronicConditions = m.group(1).trim();
            }
        }
        
        if (!allAppts.isEmpty()) {
            primaryDoctorName = "Dr. " + allAppts.stream()
                .collect(Collectors.groupingBy(a -> a.getDoctor().getFullName(), Collectors.counting()))
                .entrySet().stream()
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(java.util.Map.Entry::getKey)
                .orElse("N/A");
        }

        model.addAttribute("healthBloodGroup", bloodGroup);
        model.addAttribute("healthAllergies", allergies);
        model.addAttribute("healthEmergencyContact", emergencyContact);
        model.addAttribute("healthChronicConditions", chronicConditions);
        model.addAttribute("healthPrimaryDoctor", primaryDoctorName);

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

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping("/appointments")
    public String appointmentsPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getPatient() == null) {
            return "redirect:/login";
        }
        Long patientId = user.getPatient().getId();
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if ("upcoming".equalsIgnoreCase(timeRange)) {
            startDate = LocalDateTime.now();
        } else if ("past".equalsIgnoreCase(timeRange)) {
            endDate = LocalDateTime.now();
        } else if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                LocalDate dateVal = LocalDate.parse(dateStr);
                startDate = dateVal.atStartOfDay();
                endDate = dateVal.atTime(LocalTime.MAX);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        
        List<Appointment> appointments = appointmentRepository.searchAndFilterAppointmentsForPatient(
            patientId,
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            status != null && !status.trim().isEmpty() ? status.trim() : null,
            startDate,
            endDate
        );
        
        model.addAttribute("appointments", appointments);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("date", dateStr);
        model.addAttribute("timeRange", timeRange);
        return "patient-appointments";
    }

    @GetMapping("/appointments/book")
    public String showBookForm(@RequestParam(value = "doctorId", required = false) Long doctorId, Model model) {
        List<Doctor> doctors = doctorService.getAllDoctors();
        model.addAttribute("doctors", doctors);
        model.addAttribute("selectedDoctorId", doctorId);
        return "patient-book-form";
    }

    @PostMapping("/appointments/save")
    public String saveAppointment(@RequestParam Long doctorId,
                                  @RequestParam String appointmentDate,
                                  @RequestParam String appointmentTime,
                                  @RequestParam(required = false) String notes,
                                  HttpSession session,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            if (appointmentDate == null || appointmentDate.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment date cannot be empty.");
                return "redirect:/patient/appointments/book";
            }
            if (appointmentTime == null || appointmentTime.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment time cannot be empty.");
                return "redirect:/patient/appointments/book";
            }

            User user = (User) session.getAttribute("loggedInUser");
            Doctor doctor = doctorService.getDoctorById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid doctor Id: " + doctorId));

            LocalDate date = LocalDate.parse(appointmentDate);
            LocalTime time = LocalTime.parse(appointmentTime);
            LocalDateTime appointmentDateTime = LocalDateTime.of(date, time);

            if (date.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot book an appointment for a past date.");
                return "redirect:/patient/appointments/book";
            }

            Appointment appointment = new Appointment();
            appointment.setPatient(user.getPatient());
            appointment.setDoctor(doctor);
            appointment.setAppointmentDate(appointmentDateTime);
            appointment.setNotes(notes);
            appointment.setStatus("PENDING");

            appointmentService.saveAppointment(appointment);
            
            // Patient notification
            notificationService.createNotification(user, "PATIENT", "Appointment Booked Successfully", 
                "Your appointment with Dr. " + doctor.getFullName() + " for " + appointmentDateTime.toString() + " is registered and pending approval.", 
                "APPOINTMENT", appointment.getId());
            
            // Doctor notification
            userRepository.findByUsername(doctor.getEmail()).ifPresent(docUser -> {
                notificationService.createNotification(docUser, "DOCTOR", "New Appointment Booked", 
                    "A new appointment has been scheduled by patient " + user.getPatient().getFullName() + " for " + appointmentDateTime.toString() + ".", 
                    "APPOINTMENT", appointment.getId());
            });

            // Email confirmation to patient
            emailService.sendAppointmentConfirmation(user.getUsername(), user.getPatient().getFullName(), doctor.getFullName(), 
                appointmentDate, appointmentTime);

            redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to book appointment: " + e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/medical-records")
    public String medicalRecordsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        List<Consultation> consultations = consultationService.getConsultationsByPatient(user.getPatient().getId());
        model.addAttribute("consultations", consultations);
        
        Consultation latestConsultation = consultations.isEmpty() ? null : consultations.get(0);
        model.addAttribute("latestConsultation", latestConsultation);
        
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

    @GetMapping("/payments/pay/{id}")
    public String showDummyPaymentPage(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getPatient() == null) {
            return "redirect:/login";
        }
        
        Payment payment = paymentService.getPaymentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment Id: " + id));

        // Ownership validation
        if (!payment.getPatient().getId().equals(user.getPatient().getId())) {
            return "redirect:/patient/payments";
        }

        // Only allow paying if status is PENDING or FAILED
        if ("PAID".equals(payment.getStatus())) {
            return "redirect:/patient/payments";
        }

        model.addAttribute("payment", payment);
        return "dummy-payment";
    }

    @PostMapping("/payments/process/{id}")
    public String processDummyPayment(@PathVariable Long id,
                                      @RequestParam String paymentMethod,
                                      // Card parameters
                                      @RequestParam(required = false) String cardHolderName,
                                      @RequestParam(required = false) String cardNumber,
                                      @RequestParam(required = false) String cvv,
                                      @RequestParam(required = false) String pin,
                                      // UPI parameters
                                      @RequestParam(required = false) String upiId,
                                      @RequestParam(required = false) String upiPin,
                                      // Net Banking parameters
                                      @RequestParam(required = false) String username,
                                      @RequestParam(required = false) String password,
                                      HttpSession session,
                                      Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getPatient() == null) {
            return "redirect:/login";
        }
        
        Payment payment = paymentService.getPaymentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment Id: " + id));

        if (!payment.getPatient().getId().equals(user.getPatient().getId())) {
            return "redirect:/patient/payments";
        }

        boolean success = false;
        String errorMessage = "";

        if ("CARD".equals(paymentMethod)) {
            if (!"4111111111111111".equals(cardNumber) || !"123".equals(cvv) || !"Demo User".equalsIgnoreCase(cardHolderName)) {
                errorMessage = "Invalid Card Details. Please check your information and try again.";
            } else if (!"1234".equals(pin)) {
                errorMessage = "Incorrect PIN. Please try again.";
            } else {
                success = true;
            }
        } else if ("UPI".equals(paymentMethod)) {
            if (!"demo@upi".equalsIgnoreCase(upiId)) {
                errorMessage = "Invalid UPI ID. Please check your information and try again.";
            } else if (!"1234".equals(upiPin)) {
                errorMessage = "Incorrect PIN. Please try again.";
            } else {
                success = true;
            }
        } else if ("NET_BANKING".equals(paymentMethod)) {
            if (!"demoUser".equals(username)) {
                errorMessage = "Invalid Username. Please try again.";
            } else if (!"demo123".equals(password)) {
                errorMessage = "Incorrect Password. Please try again.";
            } else {
                success = true;
            }
        } else {
            errorMessage = "Invalid Payment Method selected.";
        }

        if (success) {
            String txnId = "TXN-" + (int)(Math.random() * 90000000 + 10000000);
            payment.setStatus("PAID");
            payment.setPaymentMethod(paymentMethod);
            payment.setTransactionId(txnId);
            payment.setPaymentDate(LocalDateTime.now());
            paymentService.savePayment(payment);

            // Patient Notification
            notificationService.createNotification(user, "PATIENT", "Payment Successful", 
                "Payment Successful. ₹" + payment.getAmount() + " has been received.", "PAYMENT", payment.getId());

            // Admin Notification
            notificationService.createNotification(null, "ADMIN", "Payment Received", 
                "Payment Received: ₹" + payment.getAmount() + " from " + user.getPatient().getFullName() + ". Transaction ID: " + txnId + ".", "PAYMENT", payment.getId());

            model.addAttribute("success", true);
            model.addAttribute("transactionId", txnId);
            model.addAttribute("paymentDate", payment.getPaymentDate());
            model.addAttribute("amount", payment.getAmount());
            model.addAttribute("message", "Your payment has been completed successfully.");
        } else {
            payment.setStatus("FAILED");
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentDate(LocalDateTime.now());
            paymentService.savePayment(payment);

            // Patient Notification
            notificationService.createNotification(user, "PATIENT", "Payment Failed", 
                "Payment Failed. Please try again.", "PAYMENT", payment.getId());

            // Admin Notification
            notificationService.createNotification(null, "ADMIN", "Payment Failed", 
                "Payment Failed: " + user.getPatient().getFullName() + " could not complete the payment.", "PAYMENT", payment.getId());

            model.addAttribute("success", false);
            model.addAttribute("errorMessage", errorMessage);
            model.addAttribute("amount", payment.getAmount());
        }

        model.addAttribute("payment", payment);
        return "payment-result";
    }

    @PostMapping("/payments/cancel/{id}")
    public String cancelDummyPayment(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getPatient() == null) {
            return "redirect:/login";
        }
        
        Payment payment = paymentService.getPaymentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment Id: " + id));

        if (!payment.getPatient().getId().equals(user.getPatient().getId())) {
            return "redirect:/patient/payments";
        }

        payment.setStatus("CANCELLED");
        payment.setPaymentDate(LocalDateTime.now());
        paymentService.savePayment(payment);

        model.addAttribute("success", false);
        model.addAttribute("errorMessage", "Payment Cancelled by User.");
        model.addAttribute("payment", payment);
        return "payment-result";
    }

    @GetMapping("/appointments/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getPatient() == null) {
            return "redirect:/login";
        }
        
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));

        // Ownership validation
        if (!appointment.getPatient().getId().equals(user.getPatient().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized to cancel this appointment.");
            return "redirect:/patient/appointments";
        }

        appointment.setStatus("CANCELLED");
        appointmentService.saveAppointment(appointment);

        // Notify Patient
        notificationService.createNotification(user, "PATIENT", "Appointment Cancelled", 
            "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " on " + appointment.getAppointmentDate().toString() + " has been cancelled.", 
            "APPOINTMENT", appointment.getId());

        // Notify Doctor
        userRepository.findByUsername(appointment.getDoctor().getEmail()).ifPresent(docUser -> {
            notificationService.createNotification(docUser, "DOCTOR", "Appointment Cancelled", 
                "Patient " + user.getPatient().getFullName() + " has cancelled their appointment on " + appointment.getAppointmentDate().toString() + ".", 
                "APPOINTMENT", appointment.getId());
        });

        // Notify Admin
        notificationService.createNotification(null, "ADMIN", "Appointment Cancelled", 
            "Appointment (ID: " + appointment.getId() + ") for patient " + user.getPatient().getFullName() + " was cancelled.", 
            "APPOINTMENT", appointment.getId());

        // Send cancellation email
        emailService.sendAppointmentCancellation(user.getUsername(), user.getPatient().getFullName(), appointment.getDoctor().getFullName(), appointment.getAppointmentDate().toString());

        redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully.");
        return "redirect:/patient/appointments";
    }

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping("/doctors")
    public String doctorDirectory(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "spec", required = false) String spec,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getPatient() == null) {
            return "redirect:/login";
        }
        
        List<Doctor> doctors = doctorRepository.searchAndFilterDoctors(
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            spec != null && !spec.trim().isEmpty() ? spec.trim() : null,
            status != null && !status.trim().isEmpty() ? status.trim() : null,
            null,
            null
        );
        
        // Fetch unique specializations
        List<String> specializations = doctorRepository.findAll().stream()
            .map(Doctor::getSpecialization)
            .filter(s -> s != null && !s.trim().isEmpty())
            .distinct()
            .toList();

        model.addAttribute("doctors", doctors);
        model.addAttribute("search", search);
        model.addAttribute("spec", spec);
        model.addAttribute("status", status);
        model.addAttribute("specializations", specializations);
        return "patient-doctors";
    }
}
