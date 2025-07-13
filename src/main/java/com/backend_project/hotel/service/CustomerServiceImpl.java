package com.backend_project.hotel.service;

import com.backend_project.hotel.model.CustomerModel;
import com.backend_project.hotel.repositories.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<?> registerCustomer(CustomerModel customer) {
        try {
            // Check if email already exists
            if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
            }

            // Check if username already exists
            if (customerRepository.findByUsername(customer.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already in use");
            }

            // Validate passwords match
            if (!customer.getPassword().equals(customer.getConfirmPassword())) {
                return ResponseEntity.badRequest().body("Passwords do not match");
            }

            // Encode password before saving
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));

            // Save the customer
            CustomerModel savedCustomer = customerRepository.save(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> loginCustomer(String username, String password) {
        try {
            Optional<CustomerModel> customer = customerRepository.findByUsername(username);
            
            if (customer.isEmpty() || !passwordEncoder.matches(password, customer.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
            }
            
            // Return customer data without password
            CustomerModel customerData = customer.get();
            customerData.setPassword(null);
            return ResponseEntity.ok(customerData);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<CustomerModel>> getAllCustomers() {
        try {
            return ResponseEntity.ok(customerRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<CustomerModel> getCustomerById(Integer userId) {
        Optional<CustomerModel> customer = customerRepository.findById(userId);
        return customer.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<?> updateCustomer(Integer userId, CustomerModel model) {
        try {
            Optional<CustomerModel> existingCustomer = customerRepository.findById(userId);
            if (existingCustomer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CustomerModel customerToUpdate = existingCustomer.get();

            // Check if email is being changed and if new email already exists
            if (!customerToUpdate.getEmail().equals(model.getEmail()) && 
                customerRepository.findByEmail(model.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
            }

            // Update fields
            customerToUpdate.setFullName(model.getFullName());
            customerToUpdate.setEmail(model.getEmail());
            customerToUpdate.setPhoneNumber(model.getPhoneNumber());
            customerToUpdate.setAddress(model.getAddress());

            // Update image only if a new one was provided
            if (model.getImage() != null && !model.getImage().isEmpty()) {
                customerToUpdate.setImage(model.getImage());
            }

            CustomerModel updatedCustomer = customerRepository.save(customerToUpdate);
            updatedCustomer.setPassword(null); // Don't return password
            return ResponseEntity.ok(updatedCustomer);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update customer: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<CustomerModel> deleteCustomer(Integer userId) {
        try {
            Optional<CustomerModel> customer = customerRepository.findById(userId);
            if (customer.isPresent()) {
                customerRepository.deleteById(userId);
                return ResponseEntity.ok(customer.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}