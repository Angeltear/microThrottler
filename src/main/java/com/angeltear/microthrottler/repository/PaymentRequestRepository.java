package com.angeltear.microthrottler.repository;

import com.angeltear.microthrottler.model.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
}