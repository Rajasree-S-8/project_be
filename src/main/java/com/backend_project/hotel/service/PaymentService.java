package com.backend_project.hotel.service;

import com.backend_project.hotel.model.PaymentModel;
import java.util.Map;

public interface PaymentService {
    String createPaymentIntent(Double amount, String currency, Integer customerId, Integer bookingId);
    void handlePaymentWebhook(Map<String, Object> payload, String sigHeader);
    PaymentModel getPaymentByTransactionId(String transactionId);
    PaymentModel processDirectPayment(Integer bookingId, Integer customerId, 
                                   Double amount, String currency,
                                   String paymentMethod, Map<String, String> paymentDetails);
}