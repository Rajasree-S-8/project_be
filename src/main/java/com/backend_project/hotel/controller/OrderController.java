package com.backend_project.hotel.controller;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

	 @Autowired
	    private OrderService orderService;

	    @PostMapping
	    public ResponseEntity<?> createOrder(
	            @RequestBody OrderRequest orderRequest,
	            @RequestHeader(name = "X-Customer-Id", required = false) Integer customerId) {
	        try {
	            if (customerId == null) {
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(Map.of("message", "Customer ID is required in X-Customer-Id header"));
	            }
	            OrderModel order = orderService.createOrder(orderRequest, customerId);
	            return ResponseEntity.status(HttpStatus.CREATED).body(order);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                    .body(Map.of("message", "Error creating order: " + e.getMessage()));
	        }
	    }

    @PostMapping("/{orderId}/confirm-payment")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Integer orderId,
            @RequestHeader(name = "X-Customer-Id") Integer customerId,
            @RequestBody Map<String, Integer> requestBody) {
        try {
            Integer paymentId = requestBody.get("paymentId");
            if (paymentId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Payment ID is required"));
            }
            orderService.confirmPayment(orderId, customerId, paymentId);
            return ResponseEntity.ok(Map.of("message", "Payment confirmed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error confirming payment: " + e.getMessage()));
        }
    }

    @GetMapping("/customer")
    public ResponseEntity<List<OrderModel>> getCustomerOrders(
            @RequestHeader(name = "X-Customer-Id") Integer customerId) {
        try {
            List<OrderModel> orders = orderService.getCustomerOrders(customerId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(
            @PathVariable Integer orderId,
            @RequestHeader(name = "X-Customer-Id") Integer customerId) {
        try {
            OrderModel order = orderService.getOrderDetails(orderId, customerId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error fetching order details: " + e.getMessage()));
        }
    }
}