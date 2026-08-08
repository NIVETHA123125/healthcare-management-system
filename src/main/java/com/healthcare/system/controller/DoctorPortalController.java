package com.healthcare.system.controller;

import com.healthcare.system.entity.*;
import com.healthcare.system.service.*;
import com.healthcare.system.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.healthcare.system.service.NotificationService;
import com.healthcare.system.service.EmailService;

@Controller
@RequestMapping("/doctor")
public class DoctorPortalController {

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
    private PrescriptionService prescriptionService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        Doctor doctor = user.getDoctor();
        Long doctorId = doctor.getId();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        // 1. Calculate statistics
        long totalPatients = appointmentRepository.countDistinctPatientsByDoctorId(doctorId);
        long todayAppts = appointmentRepository.countByDoctorIdAndAppointmentDateBetween(doctorId, startOfToday, endOfToday);
        long upcomingApptsCount = appointmentRepository.countByDoctorIdAndAppointmentDateAfter(doctorId, LocalDateTime.now());
        long completedConsultations = consultationRepository.countByDoctorId(doctorId);

        // Calculate completed but pending prescriptions
        List<Appointment> allAppts = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
        List<Appointment> completedAppts = allAppts.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .collect(Collectors.toList());
        long pendingPrescriptions = completedAppts.stream()
                .filter(a -> {
                    Optional<Consultation> consOpt = consultationRepository.findByAppointmentId(a.getId());
                    return consOpt.isEmpty() || consOpt.get().getPrescription() == null || consOpt.get().getPrescription().trim().isEmpty();
                })
                .count();

        // Notifications
        long unreadNotificationCount = notificationService.countUnread(user, "DOCTOR");
        List<Notification> recentNotifications = notificationService.getHistory(user, "DOCTOR").stream().limit(5).collect(Collectors.toList());

        model.addAttribute("totalPatients", totalPatients);
        model.addAttribute("todayAppointmentsCount", todayAppts);
        model.addAttribute("upcomingAppointmentsCount", upcomingApptsCount);
        model.addAttribute("completedConsultationsCount", completedConsultations);
        model.addAttribute("pendingPrescriptionsCount", pendingPrescriptions);
        model.addAttribute("unreadNotificationCount", unreadNotificationCount);
        model.addAttribute("recentNotifications", recentNotifications);

        // 2. Fetch today's appointments
        List<Appointment> todayList = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(doctorId, startOfToday, endOfToday);
        model.addAttribute("todayAppointments", todayList);

        // 3. Fetch next scheduled appointment
        List<Appointment> upcomingList = appointmentRepository
                .findByDoctorIdAndAppointmentDateAfterOrderByAppointmentDateAsc(doctorId, LocalDateTime.now());
        List<Appointment> nextScheduledList = upcomingList.stream()
                .filter(a -> "PENDING".equals(a.getStatus()) || "APPROVED".equals(a.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("nextAppointment", nextScheduledList.isEmpty() ? null : nextScheduledList.get(0));

        // 4. Fetch recent consultations
        List<Consultation> recentConsultations = consultationRepository.findByDoctorIdOrderByCreatedDateDesc(doctorId);
        model.addAttribute("recentConsultations", recentConsultations.stream().limit(5).collect(Collectors.toList()));

        // 5. Prescription stats
        long prescriptionsIssuedThisMonth = recentConsultations.stream()
                .filter(c -> c.getCreatedDate().getMonth() == LocalDateTime.now().getMonth() && c.getPrescription() != null && !c.getPrescription().trim().isEmpty())
                .count();
        model.addAttribute("prescriptionsIssuedThisMonth", prescriptionsIssuedThisMonth);
        model.addAttribute("prescriptionsPending", pendingPrescriptions);

        // 6. Greetings
        int hour = LocalTime.now().getHour();
        String greetingPrefix = "Good Evening";
        if (hour >= 5 && hour < 12) {
            greetingPrefix = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greetingPrefix = "Good Afternoon";
        }
        model.addAttribute("greeting", greetingPrefix + ", Dr. " + doctor.getFullName() + " 👋");
        model.addAttribute("greetingSub", "You have " + todayAppts + " appointments today and " + pendingPrescriptions + " pending prescriptions.");

        return "doctor-dashboard";
    }

    @GetMapping("/appointments")
    public String appointmentsPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String dateStr,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        Doctor doctor = user.getDoctor();
        Long doctorId = doctor.getId();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        List<Appointment> todayList;
        List<Appointment> upcomingList;

        String normSearch = search != null && !search.trim().isEmpty() ? search.trim() : null;
        String normStatus = status != null && !status.trim().isEmpty() ? status.trim() : null;

        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                LocalDate dateVal = LocalDate.parse(dateStr);
                LocalDateTime startOfDate = dateVal.atStartOfDay();
                LocalDateTime endOfDate = dateVal.atTime(LocalTime.MAX);
                
                todayList = appointmentRepository.searchAndFilterAppointmentsForDoctor(
                    doctorId, normSearch, normStatus, startOfDate, endOfDate
                );
                upcomingList = todayList;
            } catch (Exception e) {
                todayList = java.util.Collections.emptyList();
                upcomingList = java.util.Collections.emptyList();
            }
        } else {
            todayList = appointmentRepository.searchAndFilterAppointmentsForDoctor(
                doctorId, normSearch, normStatus, startOfToday, endOfToday
            );
            upcomingList = appointmentRepository.searchAndFilterAppointmentsForDoctor(
                doctorId, normSearch, normStatus, endOfToday, null
            );
        }

        model.addAttribute("todayAppointments", todayList);
        model.addAttribute("upcomingAppointments", upcomingList);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("date", dateStr);

        return "doctor-appointments";
    }

    @GetMapping("/patients")
    public String patientsPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "gender", required = false) String gender,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        Doctor doctor = user.getDoctor();
        Long doctorId = doctor.getId();

        List<Patient> patients = appointmentRepository.searchAndFilterPatientsForDoctor(
            doctorId,
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            gender != null && !gender.trim().isEmpty() ? gender.trim() : null
        );
        model.addAttribute("patients", patients);
        model.addAttribute("search", search);
        model.addAttribute("gender", gender);

        return "doctor-patients";
    }

    @GetMapping("/patients/records/{id}")
    public String viewPatientRecords(@PathVariable("id") Long patientId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Patient patient = patientService.getPatientById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID: " + patientId));

        List<Consultation> consultations = consultationService.getConsultationsByPatient(patientId);

        // Basic Profile Vitals (from the latest consultation if available)
        Consultation latestConsultation = consultations.isEmpty() ? null : consultations.get(0);
        model.addAttribute("latestConsultation", latestConsultation);

        model.addAttribute("patient", patient);
        model.addAttribute("consultations", consultations);

        // Determine if loggedInUser is a doctor or admin to check editing permission
        boolean isDoctor = "ROLE_DOCTOR".equals(user.getRole()) || "DOCTOR".equals(user.getRole());
        model.addAttribute("isDoctor", isDoctor);

        return "doctor-patient-records";
    }

    @GetMapping("/appointments/approve/{id}")
    public String approveAppointment(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));

        // Ownership validation
        if (!appointment.getDoctor().getId().equals(user.getDoctor().getId())) {
            return "redirect:/doctor/appointments";
        }

        appointment.setStatus("APPROVED");
        appointmentService.saveAppointment(appointment);

        // Notify Patient
        userRepository.findByUsername(appointment.getPatient().getEmail()).ifPresent(patientUser -> {
            notificationService.createNotification(patientUser, "PATIENT", "Appointment Approved", 
                "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " on " + appointment.getAppointmentDate().toString() + " has been approved.", 
                "APPOINTMENT", appointment.getId());
            
            // Email notification
            emailService.sendAppointmentConfirmation(patientUser.getUsername(), appointment.getPatient().getFullName(), appointment.getDoctor().getFullName(), 
                appointment.getAppointmentDate().toLocalDate().toString(), appointment.getAppointmentDate().toLocalTime().toString());
        });

        return "redirect:/doctor/appointments";
    }

    @GetMapping("/appointments/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getDoctor() == null) {
            return "redirect:/login";
        }
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id: " + id));

        // Ownership validation
        if (!appointment.getDoctor().getId().equals(user.getDoctor().getId())) {
            return "redirect:/doctor/appointments";
        }

        appointment.setStatus("CANCELLED");
        appointmentService.saveAppointment(appointment);

        // Notify Patient
        userRepository.findByUsername(appointment.getPatient().getEmail()).ifPresent(patientUser -> {
            notificationService.createNotification(patientUser, "PATIENT", "Appointment Cancelled", 
                "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " on " + appointment.getAppointmentDate().toString() + " has been cancelled by the doctor.", 
                "APPOINTMENT", appointment.getId());
            
            // Email notification
            emailService.sendAppointmentCancellation(patientUser.getUsername(), appointment.getPatient().getFullName(), appointment.getDoctor().getFullName(), appointment.getAppointmentDate().toString());
        });

        // Notify Admin
        notificationService.createNotification(null, "ADMIN", "Appointment Cancelled by Doctor", 
            "Appointment (ID: " + appointment.getId() + ") was cancelled by Dr. " + appointment.getDoctor().getFullName(), 
            "APPOINTMENT", appointment.getId());

        return "redirect:/doctor/appointments";
    }
}
