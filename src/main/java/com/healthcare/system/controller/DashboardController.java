package com.healthcare.system.controller;

import com.healthcare.system.service.AppointmentService;
import com.healthcare.system.service.DoctorService;
import com.healthcare.system.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("patientCount", patientService.getPatientCount());
        model.addAttribute("doctorCount", doctorService.getDoctorCount());
        model.addAttribute("appointmentCount", appointmentService.getAppointmentCount());
        model.addAttribute("scheduledCount", appointmentService.getScheduledCount());
        model.addAttribute("completedCount", appointmentService.getCompletedCount());
        model.addAttribute("cancelledCount", appointmentService.getCancelledCount());
        model.addAttribute("todayAppointments", appointmentService.getTodayAppointments());
        
        return "dashboard";
    }
}
