package com.healthcare.system.service;

import com.healthcare.system.entity.Doctor;
import com.healthcare.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    public Doctor saveDoctor(Doctor doctor) {
        return doctorRepository.save(doctor);
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
    public void deleteDoctor(Long id) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(id);
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();

            // 1. Delete prescriptions
            List<com.healthcare.system.entity.Prescription> prescriptions = prescriptionRepository.findByDoctorIdOrderByPrescribedDateDesc(id);
            for (com.healthcare.system.entity.Prescription pr : prescriptions) {
                prescriptionRepository.delete(pr);
            }

            // 2. Delete consultations
            List<com.healthcare.system.entity.Consultation> consultations = consultationRepository.findByDoctorIdOrderByCreatedDateDesc(id);
            for (com.healthcare.system.entity.Consultation cons : consultations) {
                consultationRepository.delete(cons);
            }

            // 3. Delete payments
            // Payments are tied to patient and appointment, but let's delete payments associated with doctor's appointments
            List<com.healthcare.system.entity.Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(id);
            for (com.healthcare.system.entity.Appointment appt : appointments) {
                // Delete payment matching this appointment if any
                // We'll search and clean up payments associated with this doctor's appointments
                // But let's delete appointments first. Cascade will clear payments if required.
                // However, since we don't have cascade direct on payments in JPA annotations, we do it here.
                paymentRepository.findAll().stream()
                    .filter(p -> p.getAppointment() != null && p.getAppointment().getId().equals(appt.getId()))
                    .forEach(p -> paymentRepository.delete(p));
                
                appointmentRepository.delete(appt);
            }

            // 4. Find associated user
            Optional<com.healthcare.system.entity.User> userOpt = userRepository.findByUsername(doctor.getEmail());
            if (userOpt.isPresent()) {
                com.healthcare.system.entity.User user = userOpt.get();

                // Delete user settings
                Optional<com.healthcare.system.entity.UserSettings> settingsOpt = userSettingsRepository.findByUserId(user.getId());
                if (settingsOpt.isPresent()) {
                    userSettingsRepository.delete(settingsOpt.get());
                }

                // Delete notifications
                List<com.healthcare.system.entity.Notification> notifications = notificationRepository.findByUserOrRoleOrderByCreatedAtDesc(user, "DOCTOR");
                for (com.healthcare.system.entity.Notification notif : notifications) {
                    notificationRepository.delete(notif);
                }

                userRepository.delete(user);
            }

            doctorRepository.delete(doctor);
        }
    }

    public List<Doctor> searchDoctors(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllDoctors();
        }
        return doctorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
            keyword, keyword, keyword
        );
    }

    public long getDoctorCount() {
        return doctorRepository.count();
    }

    public Optional<Doctor> findByEmail(String email) {
        return doctorRepository.findByEmail(email);
    }

    public Optional<Doctor> findByPhone(String phone) {
        return doctorRepository.findByPhone(phone);
    }

    public Optional<Doctor> findByEmailAndIdNot(String email, Long id) {
        return doctorRepository.findByEmailAndIdNot(email, id);
    }

    public Optional<Doctor> findByPhoneAndIdNot(String phone, Long id) {
        return doctorRepository.findByPhoneAndIdNot(phone, id);
    }
}
