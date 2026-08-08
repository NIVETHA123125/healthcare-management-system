package com.healthcare.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String specialization;

    @Column(length = 100)
    private String availabilityHours; // e.g., "09:00 AM - 05:00 PM"

    @Column(length = 10)
    private String gender;

    @Column(length = 100)
    private String qualification;

    @Column
    private Integer experience; // years

    @Column(name = "consultation_fee")
    private Double consultationFee;

    @Column(name = "availability_status", length = 20)
    private String availabilityStatus; // e.g., "Available", "Unavailable"

    @Column
    private java.time.LocalDate dob;

    @Column(columnDefinition = "TEXT")
    private String address;

    // Helper method to get full name
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
