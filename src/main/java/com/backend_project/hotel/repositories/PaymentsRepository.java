package com.backend_project.hotel.repositories;

import com.backend_project.hotel.model.PaymentsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentsRepository extends JpaRepository<PaymentsModel, Integer> {
    List<PaymentsModel> findByOrderOrderId(Integer orderId);
}