package com.backend_project.hotel.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.backend_project.hotel.model.CustomerModel;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerModel, Integer> {
    Optional<CustomerModel> findByEmail(String email);
    Optional<CustomerModel> findByUsername(String username);
}