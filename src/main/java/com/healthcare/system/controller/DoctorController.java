package com.healthcare.system.controller;

import com.healthcare.system.entity.Doctor;
import com.healthcare.system.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @GetMapping
    public String listDoctors(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Doctor> doctors = doctorService.searchDoctors(keyword);
        model.addAttribute("doctors", doctors);
        model.addAttribute("keyword", keyword);
        return "doctors";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        model.addAttribute("pageTitle", "Register New Doctor");
        return "doctor-form";
    }

    @PostMapping("/save")
    public String saveDoctor(@ModelAttribute("doctor") Doctor doctor) {
        doctorService.saveDoctor(doctor);
        return "redirect:/doctors";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Doctor doctor = doctorService.getDoctorById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid doctor Id:" + id));
        model.addAttribute("doctor", doctor);
        model.addAttribute("pageTitle", "Edit Doctor Details");
        return "doctor-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return "redirect:/doctors";
    }
}
