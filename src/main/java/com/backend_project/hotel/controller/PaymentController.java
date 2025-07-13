package com.backend_project.hotel.controller;

import com.backend_project.hotel.model.PaymentModel;
import com.backend_project.hotel.model.PaymentProcessRequest;
import com.backend_project.hotel.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class PaymentController {

    private static final Logger LOGGER = Logger.getLogger(PaymentController.class.getName());

    @Autowired
    private BookingService bookingService;

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(
            @RequestBody PaymentProcessRequest paymentRequest,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        try {
            LOGGER.info("Processing payment for booking " + paymentRequest.getBookingId() + " by customer " + customerId);
            PaymentModel payment = bookingService.processPayment(
                    paymentRequest.getBookingId(),
                    customerId,
                    paymentRequest.getAmount(),
                    paymentRequest.getCurrency(),
                    paymentRequest.getPaymentMethod(),
                    paymentRequest.getPaymentDetails()
            );
            LOGGER.info("Payment processed successfully: " + payment.getPaymentId());
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            LOGGER.severe("Error processing payment: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to process payment: " + e.getMessage()));
        }
    }
}