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
            admin.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            admin.setFullName("System Admin");
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
            System.out.println("Seeded Admin: admin / admin123");
        }

        if (userRepository.findByUsername("admin@healthcare.com").isEmpty()) {
            User admin2 = new User();
            admin2.setUsername("admin@healthcare.com");
            admin2.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("Admin@123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            admin2.setFullName("Healthcare Admin");
            admin2.setRole("ROLE_ADMIN");
            userRepository.save(admin2);
            System.out.println("Seeded Admin: admin@healthcare.com / Admin@123");
        }

        // Seed the requested 5 Doctor accounts
        for (int i = 1; i <= 5; i++) {
            String email = "doctor" + i + "@healthcare.com";
            if (userRepository.findByUsername(email).isEmpty()) {
                Doctor doc = new Doctor();
                doc.setFirstName("Doctor");
                doc.setLastName(String.valueOf(i));
                doc.setEmail(email);
                doc.setPhone("987654321" + i);
                doc.setSpecialization("General Medicine");
                doc.setAvailabilityHours("09:00 AM - 05:00 PM");
                doc.setGender("Male");
                doc.setQualification("MBBS");
                doc.setExperience(5);
                doc.setConsultationFee(100.0);
                doc.setAvailabilityStatus("Available");
                doc = doctorRepository.save(doc);

                User docUser = new User();
                docUser.setUsername(email);
                docUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("Doctor@123", org.mindrot.jbcrypt.BCrypt.gensalt()));
                docUser.setFullName("Dr. Doctor " + i);
                docUser.setRole("ROLE_DOCTOR");
                docUser.setDoctor(doc);
                userRepository.save(docUser);
                System.out.println("Seeded Doctor: " + email);
            }
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
            doctor.setGender("Male");
            doctor.setQualification("MD - Pediatrics");
            doctor.setExperience(10);
            doctor.setConsultationFee(150.0);
            doctor.setAvailabilityStatus("Available");
            doctor = doctorRepository.save(doctor);
            System.out.println("Seeded Doctor entity");
        } else {
            doctor = doctorRepository.findByEmail("shankar@caregrid.com").orElse(doctorRepository.findAll().get(0));
        }

        if (userRepository.findByUsername("doctor@caregrid.com").isEmpty()) {
            User docUser = new User();
            docUser.setUsername("doctor@caregrid.com");
            docUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("doc123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            docUser.setFullName("Dr. Shankar R");
            docUser.setRole("ROLE_DOCTOR");
            
            final Doctor finalDoc = doctor;
            boolean doctorAlreadyLinked = userRepository.findAll().stream()
                .anyMatch(u -> u.getDoctor() != null && u.getDoctor().getId().equals(finalDoc.getId()));
            if (!doctorAlreadyLinked) {
                docUser.setDoctor(finalDoc);
            }
            userRepository.save(docUser);
            System.out.println("Seeded Doctor User: doctor@caregrid.com / doc123");
        } else {
            User docUser = userRepository.findByUsername("doctor@caregrid.com").get();
            docUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("doc123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            if (docUser.getDoctor() == null) {
                // Check if this doctor is already linked to another user to avoid unique constraint violations
                final Doctor finalDoc = doctor;
                boolean doctorAlreadyLinked = userRepository.findAll().stream()
                    .anyMatch(u -> u.getDoctor() != null && u.getDoctor().getId().equals(finalDoc.getId()));
                if (!doctorAlreadyLinked) {
                    docUser.setDoctor(finalDoc);
                }
            }
            userRepository.save(docUser);
            System.out.println("Updated Doctor User's password & association");
        }

        // 3. Seed Receptionist
        if (userRepository.findByUsername("receptionist@caregrid.com").isEmpty()) {
            User recepUser = new User();
            recepUser.setUsername("receptionist@caregrid.com");
            recepUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("recep123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            recepUser.setFullName("Sarah Connor");
            recepUser.setRole("ROLE_RECEPTIONIST");
            userRepository.save(recepUser);
            System.out.println("Seeded Receptionist User: receptionist@caregrid.com / recep123");
        } else {
            User recepUser = userRepository.findByUsername("receptionist@caregrid.com").get();
            recepUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("recep123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            userRepository.save(recepUser);
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
            patient = patientRepository.findByEmail("patient@caregrid.com").orElse(patientRepository.findAll().get(0));
        }

        if (userRepository.findByUsername("patient@caregrid.com").isEmpty()) {
            User patUser = new User();
            patUser.setUsername("patient@caregrid.com");
            patUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("pat123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            patUser.setFullName("Nivetha Vivek");
            patUser.setRole("ROLE_PATIENT");
            
            final Patient finalPat = patient;
            boolean patientAlreadyLinked = userRepository.findAll().stream()
                .anyMatch(u -> u.getPatient() != null && u.getPatient().getId().equals(finalPat.getId()));
            if (!patientAlreadyLinked) {
                patUser.setPatient(finalPat);
            }
            userRepository.save(patUser);
            System.out.println("Seeded Patient User: patient@caregrid.com / pat123");
        } else {
            User patUser = userRepository.findByUsername("patient@caregrid.com").get();
            patUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("pat123", org.mindrot.jbcrypt.BCrypt.gensalt()));
            if (patUser.getPatient() == null) {
                final Patient finalPat = patient;
                boolean patientAlreadyLinked = userRepository.findAll().stream()
                    .anyMatch(u -> u.getPatient() != null && u.getPatient().getId().equals(finalPat.getId()));
                if (!patientAlreadyLinked) {
                    patUser.setPatient(finalPat);
                }
            }
            userRepository.save(patUser);
            System.out.println("Updated Patient User's password & association");
        }

        // 5. Seed Appointment, Consultation & Payment for demonstration
        if (appointmentRepository.findAll().isEmpty() && patient != null && doctor != null) {
            Appointment appt = new Appointment();
            appt.setPatient(patient);
            appt.setDoctor(doctor);
            appt.setAppointmentDate(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            appt.setStatus("APPROVED");
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
