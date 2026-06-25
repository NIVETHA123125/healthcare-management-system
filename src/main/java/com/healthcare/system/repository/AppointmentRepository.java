package com.healthcare.system.repository;

import com.healthcare.system.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderByAppointmentDateDesc();
    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Long doctorId);
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAsc(LocalDateTime start, LocalDateTime end);
    long countByStatus(String status);
}
