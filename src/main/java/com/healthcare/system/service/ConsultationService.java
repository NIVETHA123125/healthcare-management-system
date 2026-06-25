package com.healthcare.system.service;

import com.healthcare.system.entity.Consultation;
import com.healthcare.system.repository.ConsultationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ConsultationService {

    @Autowired
    private ConsultationRepository consultationRepository;

    public List<Consultation> getConsultationsByPatient(Long patientId) {
        return consultationRepository.findByPatientIdOrderByCreatedDateDesc(patientId);
    }

    public List<Consultation> getConsultationsByDoctor(Long doctorId) {
        return consultationRepository.findByDoctorIdOrderByCreatedDateDesc(doctorId);
    }

    public Optional<Consultation> getConsultationById(Long id) {
        return consultationRepository.findById(id);
    }

    public Optional<Consultation> getConsultationByAppointment(Long appointmentId) {
        return consultationRepository.findByAppointmentId(appointmentId);
    }

    public Consultation saveConsultation(Consultation consultation) {
        return consultationRepository.save(consultation);
    }
}
