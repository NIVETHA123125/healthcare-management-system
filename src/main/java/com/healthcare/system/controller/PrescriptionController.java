package com.healthcare.system.controller;

import com.healthcare.system.entity.Prescription;
import com.healthcare.system.entity.Patient;
import com.healthcare.system.entity.Doctor;
import com.healthcare.system.entity.User;
import com.healthcare.system.service.PrescriptionService;
import com.healthcare.system.service.PatientService;
import com.healthcare.system.service.DoctorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import com.healthcare.system.repository.PrescriptionRepository;

@Controller
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    // Admin/Doctor listing
    @GetMapping("/prescriptions")
    public String listPrescriptions(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            HttpSession session,
            Model model) {
        
        User user = (User) session.getAttribute("loggedInUser");
        boolean isDoctor = user != null && ("ROLE_DOCTOR".equals(user.getRole()) || "DOCTOR".equals(user.getRole()));
        
        java.time.LocalDate date = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                date = java.time.LocalDate.parse(dateStr);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        
        List<Prescription> prescriptions;
        if (isDoctor) {
            Long docId = user.getDoctor().getId();
            prescriptions = prescriptionRepository.searchAndFilterPrescriptionsForDoctor(
                docId,
                search != null && !search.trim().isEmpty() ? search.trim() : null,
                date
            );
        } else {
            prescriptions = prescriptionRepository.searchAndFilterPrescriptions(
                search != null && !search.trim().isEmpty() ? search.trim() : null,
                date,
                doctorId
            );
        }
        
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("search", search);
        model.addAttribute("date", dateStr);
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("isDoctorRole", isDoctor);
        return "prescriptions";
    }

    // Add Prescription form
    @GetMapping("/prescriptions/add")
    public String showAddForm(Model model) {
        List<Patient> patients = patientService.getAllPatients();
        List<Doctor> doctors = doctorService.getAllDoctors();
        model.addAttribute("patients", patients);
        model.addAttribute("doctors", doctors);
        model.addAttribute("prescription", new Prescription());
        model.addAttribute("pageTitle", "New Prescription");
        return "prescription-form";
    }

    // Save Prescription with backend validations
    @PostMapping("/prescriptions/save")
    public String savePrescription(@ModelAttribute("prescription") Prescription prescription,
                                   @RequestParam("patientId") Long patientId,
                                   @RequestParam("doctorId") Long doctorId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        boolean hasErrors = false;

        if (prescription.getMedication() == null || prescription.getMedication().trim().isEmpty()) {
            model.addAttribute("medicationError", "Medication name cannot be empty.");
            hasErrors = true;
        }

        if (prescription.getDosage() == null || prescription.getDosage().trim().isEmpty()) {
            model.addAttribute("dosageError", "Dosage instructions cannot be empty.");
            hasErrors = true;
        }

        if (prescription.getDuration() == null || prescription.getDuration().trim().isEmpty()) {
            model.addAttribute("durationError", "Duration cannot be empty.");
            hasErrors = true;
        }

        if (hasErrors) {
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("doctors", doctorService.getAllDoctors());
            model.addAttribute("prescription", prescription);
            model.addAttribute("selectedPatientId", patientId);
            model.addAttribute("selectedDoctorId", doctorId);
            model.addAttribute("pageTitle", "New Prescription");
            return "prescription-form";
        }

        try {
            Patient patient = patientService.getPatientById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID: " + patientId));
            Doctor doctor = doctorService.getDoctorById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid doctor ID: " + doctorId));
            
            prescription.setPatient(patient);
            prescription.setDoctor(doctor);
            prescriptionService.savePrescription(prescription);

            redirectAttributes.addFlashAttribute("successMessage", "Prescription added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving prescription: " + e.getMessage());
        }

        return "redirect:/prescriptions";
    }

    // Delete Prescription
    @GetMapping("/prescriptions/delete/{id}")
    public String deletePrescription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.deletePrescription(id);
            redirectAttributes.addFlashAttribute("successMessage", "Prescription deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting prescription: " + e.getMessage());
        }
        return "redirect:/prescriptions";
    }

    // Patient Portal: My Prescriptions listing
    @GetMapping("/patient/prescriptions")
    public String listPatientPrescriptions(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "date", required = false) String dateStr,
            HttpSession session,
            Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || loggedInUser.getPatient() == null) {
            return "redirect:/login";
        }
        
        java.time.LocalDate date = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                date = java.time.LocalDate.parse(dateStr);
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        
        List<Prescription> prescriptions = prescriptionRepository.searchAndFilterPrescriptionsForPatient(
            loggedInUser.getPatient().getId(),
            search != null && !search.trim().isEmpty() ? search.trim() : null,
            date
        );
        
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("search", search);
        model.addAttribute("date", dateStr);
        return "patient-prescriptions";
    }

    @GetMapping("/patient/prescriptions/print/{id}")
    public String printPrescription(@PathVariable Long id, HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        Prescription prescription = prescriptionService.getPrescriptionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid prescription ID: " + id));
        
        // Security check: patients can only print their own prescriptions, but doctors/admins can also print
        boolean isPatient = "ROLE_PATIENT".equals(loggedInUser.getRole()) || "PATIENT".equals(loggedInUser.getRole());
        if (isPatient && !prescription.getPatient().getId().equals(loggedInUser.getPatient().getId())) {
            return "redirect:/patient/prescriptions";
        }
        
        model.addAttribute("prescription", prescription);
        return "prescription-print";
    }
}
