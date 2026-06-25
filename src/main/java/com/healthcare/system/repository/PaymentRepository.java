package com.healthcare.system.repository;

import com.healthcare.system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPatientIdOrderByPaymentDateDesc(Long patientId);
    List<Payment> findAllByOrderByPaymentDateDesc();
}
