package com.backend_project.hotel.controller;

import com.backend_project.hotel.model.CompletePaymentRequest;
import com.backend_project.hotel.model.PaymentModel;
import com.backend_project.hotel.model.PaymentProcessRequest;
import com.backend_project.hotel.service.PaymentService;
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
    private PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(
            @RequestBody PaymentProcessRequest paymentRequest,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        try {
            LOGGER.info("Processing payment for booking " + paymentRequest.getBookingId() + " by customer " + customerId);
            
            PaymentModel payment = paymentService.processDirectPayment(
                    paymentRequest.getBookingId(),
                    customerId,
                    paymentRequest.getAmount(),
                    paymentRequest.getCurrency(),
                    paymentRequest.getPaymentMethod(),
                    paymentRequest.getPaymentDetails()
            );
            
            LOGGER.info("Payment processed successfully: " + payment.getTransactionId());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "paymentId", payment.getPaymentId(),
                    "transactionId", payment.getTransactionId()
            ));
        } catch (RuntimeException e) {
            LOGGER.warning("Payment processing error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Error processing payment: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", "Failed to process payment"));
        }
    }
    @PostMapping("/complete")
    public ResponseEntity<?> completePayment(
        @RequestBody CompletePaymentRequest request,
        @RequestHeader("X-Customer-Id") Integer customerId) {
      try {
        PaymentModel payment = paymentService.completePendingPayment(
          request.getBookingId(),
          customerId,
          request.getPaymentDetails()
        );
        
        return ResponseEntity.ok(Map.of(
          "status", "success",
          "paymentId", payment.getPaymentId(),
          "transactionId", payment.getTransactionId()
        ));
      } catch (RuntimeException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("status", "error", "message", e.getMessage()));
      }
    }
}