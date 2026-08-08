package com.healthcare.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Appearance & Localization
    @Column(length = 20)
    private String theme = "SYSTEM"; // LIGHT, DARK, SYSTEM

    @Column(length = 10)
    private String language = "en"; // en, ta, hi

    // Accessibility
    @Column(length = 20)
    private String fontSize = "MEDIUM"; // SMALL, MEDIUM, LARGE

    private boolean highContrast = false;
    private boolean reduceMotion = false;
    private boolean keyboardNavigation = false;

    // Notifications
    private boolean emailNotifications = true;
    private boolean smsNotifications = true;
    private boolean appointmentReminders = true;
    private boolean systemAnnouncements = true;
    private boolean pushNotifications = false;

    // Privacy
    @Column(length = 20)
    private String personalInfoVisibility = "PUBLIC"; // PUBLIC, PRIVATE, MUTUAL
    
    // Patient specific settings
    private String preferredHospital = "";
    private String preferredDoctor = "";
    private String emergencyContact = "";
    private String bloodGroup = "";
    private String allergies = "";
    @Column(columnDefinition = "TEXT")
    private String medicalNotes = "";
    private String defaultConsultationMode = "OFFLINE"; // ONLINE, OFFLINE

    // Doctor specific settings
    private String specialization = "";
    private Double consultationFee = 0.0;
    private String availableDays = "Monday,Tuesday,Wednesday,Thursday,Friday";
    private String availableTimeSlots = "09:00 AM - 05:00 PM";
    private Integer consultationDuration = 15; // in minutes
    private boolean onlineConsultation = false;
    private String clinicName = "";
    private String clinicAddress = "";
    private String clinicContactNumber = "";

    // Admin specific settings
    private String hospitalName = "CareGrid Healthcare";
    private String hospitalLogo = "";
    private String defaultTimeZone = "GMT+5:30";
    private String defaultLanguage = "en";
    private String defaultCurrency = "INR (₹)";
    private String defaultDateFormat = "dd-MMM-yyyy";
    private String defaultPatientStatus = "ACTIVE";
    private String defaultDoctorStatus = "ACTIVE";
    private String registrationApprovalSettings = "AUTO"; // AUTO, MANUAL
    private Integer sessionTimeout = 30; // in minutes
    private String passwordPolicy = "STRONG"; // STANDARD, STRONG
    private boolean twoFactorAuth = false;
    private Integer maxLoginAttempts = 5;
}
