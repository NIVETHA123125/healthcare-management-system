package com.healthcare.system.repository;

import com.healthcare.system.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String firstName, String lastName, String email
    );

    java.util.Optional<Patient> findByEmail(String email);
    java.util.Optional<Patient> findByPhone(String phone);
    java.util.Optional<Patient> findByEmailAndIdNot(String email, Long id);
    java.util.Optional<Patient> findByPhoneAndIdNot(String phone, Long id);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Patient p WHERE " +
       "(:search IS NULL OR :search = '' OR " +
       " LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       " p.phone LIKE CONCAT('%', :search, '%') OR " +
       " CAST(p.id AS string) LIKE CONCAT('%', :search, '%')) " +
       "AND (:gender IS NULL OR :gender = '' OR p.gender = :gender) " +
       "AND (:regDate IS NULL OR p.registrationDate = :regDate) " +
       "AND (:minAge IS NULL OR (YEAR(CURRENT_DATE) - YEAR(p.dob)) >= :minAge) " +
       "AND (:maxAge IS NULL OR (YEAR(CURRENT_DATE) - YEAR(p.dob)) <= :maxAge)")
    List<Patient> searchAndFilterPatients(
        @org.springframework.data.repository.query.Param("search") String search,
        @org.springframework.data.repository.query.Param("gender") String gender,
        @org.springframework.data.repository.query.Param("regDate") java.time.LocalDate regDate,
        @org.springframework.data.repository.query.Param("minAge") Integer minAge,
        @org.springframework.data.repository.query.Param("maxAge") Integer maxAge
    );
}
