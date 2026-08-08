package com.healthcare.system.repository;

import com.healthcare.system.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByPrescribedDateDesc(Long patientId);
    List<Prescription> findByDoctorIdOrderByPrescribedDateDesc(Long doctorId);
    List<Prescription> findAllByOrderByPrescribedDateDesc();

    @org.springframework.data.jpa.repository.Query("SELECT pr FROM Prescription pr WHERE " +
       "(:search IS NULL OR :search = '' OR " +
       " LOWER(pr.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(pr.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(CONCAT(pr.patient.firstName, ' ', pr.patient.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(pr.doctor.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(pr.doctor.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(CONCAT(pr.doctor.firstName, ' ', pr.doctor.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " CAST(pr.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:date IS NULL OR pr.prescribedDate = :date) " +
       "AND (:doctorId IS NULL OR pr.doctor.id = :doctorId) " +
       "ORDER BY pr.prescribedDate DESC")
    List<Prescription> searchAndFilterPrescriptions(
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
        @org.springframework.data.repository.query.Param("doctorId") Long doctorId
    );

    @org.springframework.data.jpa.repository.Query("SELECT pr FROM Prescription pr WHERE pr.doctor.id = :doctorId " +
       "AND (:search IS NULL OR :search = '' OR " +
       "     LOWER(pr.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(pr.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(CONCAT(pr.patient.firstName, ' ', pr.patient.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     CAST(pr.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:date IS NULL OR pr.prescribedDate = :date) " +
       "ORDER BY pr.prescribedDate DESC")
    List<Prescription> searchAndFilterPrescriptionsForDoctor(
        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("date") java.time.LocalDate date
    );

    @org.springframework.data.jpa.repository.Query("SELECT pr FROM Prescription pr WHERE pr.patient.id = :patientId " +
       "AND (:search IS NULL OR :search = '' OR " +
       "     LOWER(pr.doctor.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(pr.doctor.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     LOWER(CONCAT(pr.doctor.firstName, ' ', pr.doctor.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "     CAST(pr.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:date IS NULL OR pr.prescribedDate = :date) " +
       "ORDER BY pr.prescribedDate DESC")
    List<Prescription> searchAndFilterPrescriptionsForPatient(
        @org.springframework.data.repository.query.Param("patientId") Long patientId,
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("date") java.time.LocalDate date
    );
}
