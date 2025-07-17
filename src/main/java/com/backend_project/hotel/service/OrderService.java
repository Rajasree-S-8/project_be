package com.backend_project.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

public interface OrderService {
    ResponseEntity<?> createOrder(JsonNode orderRequest, Integer customerId);
    ResponseEntity<?> processPayment(Integer orderId, JsonNode paymentRequest, Integer customerId);
    ResponseEntity<?> getOrderDetails(Integer orderId, Integer customerId);
    ResponseEntity<?> getCustomerOrders(Integer customerId);
    ResponseEntity<byte[]> generateInvoice(Integer orderId, Integer customerId);
    ResponseEntity<?> cancelOrder(Integer orderId, Integer customerId);
}