package com.backend_project.hotel.controller;

import com.backend_project.hotel.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody JsonNode orderRequest,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        return orderService.createOrder(orderRequest, customerId);
    }

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<?> processPayment(
            @PathVariable Integer orderId,
            @RequestBody JsonNode paymentRequest,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        return orderService.processPayment(orderId, paymentRequest, customerId);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(
            @PathVariable Integer orderId,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        return orderService.getOrderDetails(orderId, customerId);
    }

    @GetMapping
    public ResponseEntity<?> getCustomerOrders(
            @RequestHeader("X-Customer-Id") Integer customerId) {
        return orderService.getCustomerOrders(customerId);
    }
}