package com.healthcare.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String prescription;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Basic Information
    @Column(length = 20)
    private String height;

    @Column(length = 20)
    private String weight;

    @Column(length = 20)
    private String bloodGroup;

    @Column(length = 255)
    private String bloodPressure;

    // Medical History
    @Column(columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(columnDefinition = "TEXT")
    private String currentSymptoms;

    @Column(columnDefinition = "TEXT")
    private String previousDiseases;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String currentMedications;

    @Column(columnDefinition = "TEXT")
    private String previousMedications;

    @Column(columnDefinition = "TEXT")
    private String surgeries;

    @Column(columnDefinition = "TEXT")
    private String familyMedicalHistory;

    @Column(columnDefinition = "TEXT")
    private String lifestyleInfo;

    // Consultation Notes
    @Column(columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(columnDefinition = "TEXT")
    private String followUpInstructions;

    // Prescription Section (detailed fields)
    @Column(columnDefinition = "TEXT")
    private String medicineName;

    @Column(columnDefinition = "TEXT")
    private String dosage;

    @Column(columnDefinition = "TEXT")
    private String frequency;

    @Column(columnDefinition = "TEXT")
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}
