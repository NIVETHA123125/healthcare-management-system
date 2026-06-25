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
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @GetMapping
    public String listAppointments(Model model) {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        model.addAttribute("appointments", appointments);
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
                                  @RequestParam("appointmentDate") String appointmentDateStr,
                                  @RequestParam(value = "notes", required = false) String notes) {
        
        Patient patient = patientService.getPatientById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient Id: " + patientId));
        Doctor doctor = doctorService.getDoctorById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid doctor Id: " + doctorId));
        
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(LocalDateTime.parse(appointmentDateStr));
        appointment.setNotes(notes);
        appointment.setStatus("SCHEDULED");
        
        appointmentService.saveAppointment(appointment);
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
        return "redirect:/appointments";
    }

    @GetMapping("/history")
    public String appointmentHistory(Model model) {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        model.addAttribute("appointments", appointments);
        return "history";
    }
}
