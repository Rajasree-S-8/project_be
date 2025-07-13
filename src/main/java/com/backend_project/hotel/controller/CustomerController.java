package com.backend_project.hotel.controller;

import com.backend_project.hotel.model.CustomerModel;
import com.backend_project.hotel.model.LoginRequest;
import com.backend_project.hotel.service.CustomerService;
import com.backend_project.hotel.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomerController {

    @Autowired
    private CustomerService customerService;
//
    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam("username") String username,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address) {
        
        try {
            CustomerModel customer = new CustomerModel();
            customer.setUsername(username);
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setPassword(password);
            customer.setConfirmPassword(confirmPassword);
            customer.setPhoneNumber(phone);
            customer.setAddress(address);

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                if (imageFile.getSize() > 5048576) { // 5MB limit
                    return ResponseEntity.badRequest().body("Image size must be less than 5MB");
                }
                if (!imageFile.getContentType().startsWith("image/")) {
                    return ResponseEntity.badRequest().body("Only image files are allowed");
                }
                
                String storedFilename = fileStorageService.storeFile(imageFile);
                customer.setImage(storedFilename);
            }

            return customerService.registerCustomer(customer);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginCustomer(@RequestBody LoginRequest loginRequest) {
        return customerService.loginCustomer(loginRequest.getUsername(), loginRequest.getPassword());
    }

    @GetMapping("/all")
    public ResponseEntity<List<CustomerModel>> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CustomerModel> getCustomerById(@PathVariable Integer userId) {
        return customerService.getCustomerById(userId);
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer userId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address) {
        
        try {
            CustomerModel customer = new CustomerModel();
            customer.setUserId(userId);
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setPhoneNumber(phone);
            customer.setAddress(address);

            // Handle image upload if new image is provided
            if (imageFile != null && !imageFile.isEmpty()) {
                if (imageFile.getSize() > 5048576) { // 5MB limit
                    return ResponseEntity.badRequest().body("Image size must be less than 5MB");
                }
                if (!imageFile.getContentType().startsWith("image/")) {
                    return ResponseEntity.badRequest().body("Only image files are allowed");
                }
                
                String storedFilename = fileStorageService.storeFile(imageFile);
                customer.setImage(storedFilename);
            }

            return customerService.updateCustomer(userId, customer);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Update failed: " + e.getMessage());
        }
    }
    

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<CustomerModel> deleteCustomer(@PathVariable Integer userId) {
        return customerService.deleteCustomer(userId);
    }
}