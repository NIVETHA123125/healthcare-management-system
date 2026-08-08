package com.healthcare.system.repository;

import com.healthcare.system.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
        String firstName, String lastName, String specialization
    );

    java.util.Optional<Doctor> findByEmail(String email);
    java.util.Optional<Doctor> findByPhone(String phone);
    java.util.Optional<Doctor> findByEmailAndIdNot(String email, Long id);
    java.util.Optional<Doctor> findByPhoneAndIdNot(String phone, Long id);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Doctor d WHERE " +
       "(:search IS NULL OR :search = '' OR " +
       " LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " d.phone LIKE CONCAT('%', :search, '%') OR " +
       " CAST(d.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:spec IS NULL OR :spec = '' OR d.specialization = :spec) " +
       "AND (:status IS NULL OR :status = '' OR d.availabilityStatus = :status) " +
       "AND (:minExp IS NULL OR d.experience >= :minExp) " +
       "AND (:maxExp IS NULL OR d.experience <= :maxExp)")
    List<Doctor> searchAndFilterDoctors(
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("spec") String specialization,
        @org.springframework.data.repository.query.Param("status") String status,
        @org.springframework.data.repository.query.Param("minExp") Integer minExp,
        @org.springframework.data.repository.query.Param("maxExp") Integer maxExp
    );
}
