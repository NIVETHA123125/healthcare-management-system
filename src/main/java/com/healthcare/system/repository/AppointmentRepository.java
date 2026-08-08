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
    List<Appointment> findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(Long doctorId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByDoctorIdAndAppointmentDateAfterOrderByAppointmentDateAsc(Long doctorId, LocalDateTime dateTime);
    long countByDoctorIdAndAppointmentDateBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    long countByDoctorIdAndAppointmentDateAfter(Long doctorId, LocalDateTime dateTime);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId")
    long countDistinctPatientsByDoctorId(@org.springframework.data.repository.query.Param("doctorId") Long doctorId);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.patient FROM Appointment a WHERE a.doctor.id = :doctorId")
    List<com.healthcare.system.entity.Patient> findDistinctPatientsByDoctorId(@org.springframework.data.repository.query.Param("doctorId") Long doctorId);
    
    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Appointment a WHERE " +
       "(:search IS NULL OR :search = '' OR " +
       " LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(CONCAT(a.patient.firstName, ' ', a.patient.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(a.doctor.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(a.doctor.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(CONCAT(a.doctor.firstName, ' ', a.doctor.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " CAST(a.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
       "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
       "AND (:startDate IS NULL OR a.appointmentDate >= :startDate) " +
       "AND (:endDate IS NULL OR a.appointmentDate <= :endDate) " +
       "ORDER BY a.appointmentDate DESC")
    List<Appointment> searchAndFilterAppointments(
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("status") String status,
        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
        @org.springframework.data.repository.query.Param("startDate") LocalDateTime startDate,
        @org.springframework.data.repository.query.Param("endDate") LocalDateTime endDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
       "AND (:search IS NULL OR :search = '' OR " +
       "     LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(CONCAT(a.patient.firstName, ' ', a.patient.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     CAST(a.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
       "AND (:startDate IS NULL OR a.appointmentDate >= :startDate) " +
       "AND (:endDate IS NULL OR a.appointmentDate <= :endDate) " +
       "ORDER BY a.appointmentDate DESC")
    List<Appointment> searchAndFilterAppointmentsForDoctor(
        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("status") String status,
        @org.springframework.data.repository.query.Param("startDate") LocalDateTime startDate,
        @org.springframework.data.repository.query.Param("endDate") LocalDateTime endDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId " +
       "AND (:search IS NULL OR :search = '' OR " +
       "     LOWER(a.doctor.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(a.doctor.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(CONCAT(a.doctor.firstName, ' ', a.doctor.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     CAST(a.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
       "AND (:startDate IS NULL OR a.appointmentDate >= :startDate) " +
       "AND (:endDate IS NULL OR a.appointmentDate <= :endDate) " +
       "ORDER BY a.appointmentDate DESC")
    List<Appointment> searchAndFilterAppointmentsForPatient(
        @org.springframework.data.repository.query.Param("patientId") Long patientId,
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("status") String status,
        @org.springframework.data.repository.query.Param("startDate") LocalDateTime startDate,
        @org.springframework.data.repository.query.Param("endDate") LocalDateTime endDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.patient FROM Appointment a WHERE a.doctor.id = :doctorId " +
       "AND (:search IS NULL OR :search = '' OR " +
       "     LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(CONCAT(a.patient.firstName, ' ', a.patient.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(a.patient.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     a.patient.phone LIKE CONCAT('%', :search, '%')) " +
       "AND (:gender IS NULL OR :gender = '' OR a.patient.gender = :gender)")
    List<com.healthcare.system.entity.Patient> searchAndFilterPatientsForDoctor(
        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("gender") String gender
    );
}
