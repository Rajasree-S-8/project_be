package com.backend_project.hotel.repositories;

import com.backend_project.hotel.model.PaymentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentModel, Integer> {
    PaymentModel findByTransactionId(String transactionId);
    
}