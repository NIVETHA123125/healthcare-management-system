package com.healthcare.system.service;

import com.healthcare.system.entity.Appointment;
import com.healthcare.system.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateDesc();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment saveAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
    }

    public List<Appointment> getTodayAppointments() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return appointmentRepository.findByAppointmentDateBetweenOrderByAppointmentDateAsc(start, end);
    }

    public long getAppointmentCount() {
        return appointmentRepository.count();
    }

    public long getScheduledCount() {
        return appointmentRepository.countByStatus("SCHEDULED");
    }

    public long getCompletedCount() {
        return appointmentRepository.countByStatus("COMPLETED");
    }

    public long getCancelledCount() {
        return appointmentRepository.countByStatus("CANCELLED");
    }
}
