package com.healthcare.system.controller;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.entity.Payment;
import com.healthcare.system.service.PatientService;
import com.healthcare.system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PatientService patientService;

    @GetMapping
    public String listPayments(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        model.addAttribute("payments", payments);
        return "payments";
    }

    @GetMapping("/add")
    public String showAddBillForm(Model model) {
        List<Patient> patients = patientService.getAllPatients();
        model.addAttribute("patients", patients);
        model.addAttribute("payment", new Payment());
        return "payment-form";
    }

    @PostMapping("/save")
    public String savePayment(@ModelAttribute("payment") Payment payment, @RequestParam("patientId") Long patientId) {
        Patient patient = patientService.getPatientById(patientId).orElseThrow();
        payment.setPatient(patient);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        paymentService.savePayment(payment);
        return "redirect:/payments";
    }

    @GetMapping("/edit/{id}")
    public String showEditBillForm(@PathVariable Long id, Model model) {
        Payment payment = paymentService.getPaymentById(id).orElseThrow();
        List<Patient> patients = patientService.getAllPatients();
        model.addAttribute("payment", payment);
        model.addAttribute("patients", patients);
        return "payment-form";
    }

    @GetMapping("/delete/{id}")
    public String deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return "redirect:/payments";
    }
}
