package com.healthcare.system.service;

import com.healthcare.system.entity.Doctor;
import com.healthcare.system.repository.DoctorRepository;
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

    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
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
}
