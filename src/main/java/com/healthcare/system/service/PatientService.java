package com.healthcare.system.service;

import com.healthcare.system.entity.Patient;
import com.healthcare.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @org.springframework.transaction.annotation.Transactional
    public void deletePatient(Long id) {
        Optional<Patient> patientOpt = patientRepository.findById(id);
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();

            // 1. Delete prescriptions
            List<com.healthcare.system.entity.Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByPrescribedDateDesc(id);
            for (com.healthcare.system.entity.Prescription pr : prescriptions) {
                prescriptionRepository.delete(pr);
            }

            // 2. Delete consultations
            List<com.healthcare.system.entity.Consultation> consultations = consultationRepository.findByPatientIdOrderByCreatedDateDesc(id);
            for (com.healthcare.system.entity.Consultation cons : consultations) {
                consultationRepository.delete(cons);
            }

            // 3. Delete payments
            List<com.healthcare.system.entity.Payment> payments = paymentRepository.findByPatientIdOrderByPaymentDateDesc(id);
            for (com.healthcare.system.entity.Payment pay : payments) {
                paymentRepository.delete(pay);
            }

            // 4. Delete appointments
            List<com.healthcare.system.entity.Appointment> appointments = appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(id);
            for (com.healthcare.system.entity.Appointment appt : appointments) {
                appointmentRepository.delete(appt);
            }

            // 5. Find associated user
            Optional<com.healthcare.system.entity.User> userOpt = userRepository.findByUsername(patient.getEmail());
            if (userOpt.isPresent()) {
                com.healthcare.system.entity.User user = userOpt.get();
                
                // Delete user settings
                Optional<com.healthcare.system.entity.UserSettings> settingsOpt = userSettingsRepository.findByUserId(user.getId());
                if (settingsOpt.isPresent()) {
                    userSettingsRepository.delete(settingsOpt.get());
                }

                // Delete notifications
                List<com.healthcare.system.entity.Notification> notifications = notificationRepository.findByUserOrRoleOrderByCreatedAtDesc(user, "PATIENT");
                for (com.healthcare.system.entity.Notification notif : notifications) {
                    notificationRepository.delete(notif);
                }

                userRepository.delete(user);
            }

            patientRepository.delete(patient);
        }
    }

    public List<Patient> searchPatients(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPatients();
        }
        return patientRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            keyword, keyword, keyword
        );
    }

    public long getPatientCount() {
        return patientRepository.count();
    }

    public Optional<Patient> findByEmail(String email) {
        return patientRepository.findByEmail(email);
    }

    public Optional<Patient> findByPhone(String phone) {
        return patientRepository.findByPhone(phone);
    }

    public Optional<Patient> findByEmailAndIdNot(String email, Long id) {
        return patientRepository.findByEmailAndIdNot(email, id);
    }

    public Optional<Patient> findByPhoneAndIdNot(String phone, Long id) {
        return patientRepository.findByPhoneAndIdNot(phone, id);
    }
}
