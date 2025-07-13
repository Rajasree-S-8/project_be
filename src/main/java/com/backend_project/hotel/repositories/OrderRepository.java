package com.backend_project.hotel.repositories;

import com.backend_project.hotel.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderModel, Integer> {
    List<OrderModel> findByCustomerUserId(Integer customerId);
}