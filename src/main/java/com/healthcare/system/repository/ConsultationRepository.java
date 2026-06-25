package com.healthcare.system.repository;

import com.healthcare.system.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByPatientIdOrderByCreatedDateDesc(Long patientId);
    List<Consultation> findByDoctorIdOrderByCreatedDateDesc(Long doctorId);
    Optional<Consultation> findByAppointmentId(Long appointmentId);
}
