package com.healthcare.system.controller;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping
    public String listPatients(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Patient> patients = patientService.searchPatients(keyword);
        model.addAttribute("patients", patients);
        model.addAttribute("keyword", keyword);
        return "patients";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("patient", new Patient());
        model.addAttribute("pageTitle", "Register New Patient");
        return "patient-form";
    }

    @PostMapping("/save")
    public String savePatient(@ModelAttribute("patient") Patient patient) {
        patientService.savePatient(patient);
        return "redirect:/patients";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Patient patient = patientService.getPatientById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient Id:" + id));
        model.addAttribute("patient", patient);
        model.addAttribute("pageTitle", "Edit Patient Details");
        return "patient-form";
    }

    @GetMapping("/delete/{id}")
    public String deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return "redirect:/patients";
    }
}
