package com.backend_project.hotel.service;

import com.backend_project.hotel.model.CustomerModel;
import org.springframework.http.ResponseEntity;
import java.util.List;

public interface CustomerService {
    ResponseEntity<?> registerCustomer(CustomerModel customer);
    ResponseEntity<?> loginCustomer(String email, String password);
    ResponseEntity<List<CustomerModel>> getAllCustomers();
    ResponseEntity<CustomerModel> getCustomerById(Integer userId);
    ResponseEntity<?> updateCustomer(Integer userId, CustomerModel model);
    ResponseEntity<CustomerModel> deleteCustomer(Integer userId);
}