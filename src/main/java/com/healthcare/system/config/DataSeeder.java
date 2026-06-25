package com.healthcare.system.config;

import com.healthcare.system.entity.*;
import com.healthcare.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Admin User
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setFullName("System Admin");
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
            System.out.println("Seeded Admin: admin / admin123");
        }

        // 2. Seed Doctor
        Doctor doctor = null;
        if (doctorRepository.findAll().isEmpty()) {
            doctor = new Doctor();
            doctor.setFirstName("Shankar");
            doctor.setLastName("R");
            doctor.setEmail("shankar@caregrid.com");
            doctor.setPhone("1234567890");
            doctor.setSpecialization("Pediatrician");
            doctor.setAvailabilityHours("05:00 PM - 10:00 PM");
            doctor = doctorRepository.save(doctor);
            System.out.println("Seeded Doctor entity");
        } else {
            doctor = doctorRepository.findAll().get(0);
        }

        if (userRepository.findByUsername("doctor@caregrid.com").isEmpty()) {
            User docUser = new User();
            docUser.setUsername("doctor@caregrid.com");
            docUser.setPassword("doc123");
            docUser.setFullName("Dr. Shankar R");
            docUser.setRole("ROLE_DOCTOR");
            docUser.setDoctor(doctor);
            userRepository.save(docUser);
            System.out.println("Seeded Doctor User: doctor@caregrid.com / doc123");
        }

        // 3. Seed Receptionist
        if (userRepository.findByUsername("receptionist@caregrid.com").isEmpty()) {
            User recepUser = new User();
            recepUser.setUsername("receptionist@caregrid.com");
            recepUser.setPassword("recep123");
            recepUser.setFullName("Sarah Connor");
            recepUser.setRole("ROLE_RECEPTIONIST");
            userRepository.save(recepUser);
            System.out.println("Seeded Receptionist User: receptionist@caregrid.com / recep123");
        }

        // 4. Seed Patient
        Patient patient = null;
        if (patientRepository.findAll().isEmpty()) {
            patient = new Patient();
            patient.setFirstName("Nivetha");
            patient.setLastName("Vivek");
            patient.setEmail("patient@caregrid.com");
            patient.setPhone("9876543210");
            patient.setDob(LocalDate.of(1998, 6, 20));
            patient.setGender("Female");
            patient.setAddress("123 Care Street, City");
            patient.setMedicalHistory("Mild seasonal allergies");
            patient = patientRepository.save(patient);
            System.out.println("Seeded Patient entity");
        } else {
            patient = patientRepository.findAll().get(0);
        }

        if (userRepository.findByUsername("patient@caregrid.com").isEmpty()) {
            User patUser = new User();
            patUser.setUsername("patient@caregrid.com");
            patUser.setPassword("pat123");
            patUser.setFullName("Nivetha Vivek");
            patUser.setRole("ROLE_PATIENT");
            patUser.setPatient(patient);
            userRepository.save(patUser);
            System.out.println("Seeded Patient User: patient@caregrid.com / pat123");
        }

        // 5. Seed Appointment, Consultation & Payment for demonstration
        if (appointmentRepository.findAll().isEmpty() && patient != null && doctor != null) {
            Appointment appt = new Appointment();
            appt.setPatient(patient);
            appt.setDoctor(doctor);
            appt.setAppointmentDate(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            appt.setStatus("SCHEDULED");
            appt.setNotes("Regular health checkup");
            appt = appointmentRepository.save(appt);

            Appointment pastAppt = new Appointment();
            pastAppt.setPatient(patient);
            pastAppt.setDoctor(doctor);
            pastAppt.setAppointmentDate(LocalDateTime.now().minusDays(2).withHour(11).withMinute(0));
            pastAppt.setStatus("COMPLETED");
            pastAppt.setNotes("Follow-up on fever symptoms");
            pastAppt = appointmentRepository.save(pastAppt);

            // Consultation notes
            Consultation consultation = new Consultation();
            consultation.setPatient(patient);
            consultation.setDoctor(doctor);
            consultation.setAppointment(pastAppt);
            consultation.setDiagnosis("Mild influenza recovery");
            consultation.setPrescription("Paracetamol 500mg, Rest, Hydration");
            consultation.setNotes("Patient is recovering well. Keep hydrated.");
            consultation.setCreatedDate(LocalDateTime.now().minusDays(2));
            consultationRepository.save(consultation);

            // Payment log
            Payment payment = new Payment();
            payment.setPatient(patient);
            payment.setAppointment(pastAppt);
            payment.setAmount(150.0);
            payment.setPaymentDate(LocalDateTime.now().minusDays(2));
            payment.setStatus("PAID");
            payment.setPaymentMethod("CARD");
            paymentRepository.save(payment);

            Payment pendingPayment = new Payment();
            pendingPayment.setPatient(patient);
            pendingPayment.setAppointment(appt);
            pendingPayment.setAmount(150.0);
            pendingPayment.setPaymentDate(LocalDateTime.now().plusDays(1));
            pendingPayment.setStatus("PENDING");
            pendingPayment.setPaymentMethod("CASH");
            paymentRepository.save(pendingPayment);

            System.out.println("Seeded dummy appointments, consultation logs, and payments");
        }
    }
}
