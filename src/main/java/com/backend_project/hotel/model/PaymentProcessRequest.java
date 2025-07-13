package com.backend_project.hotel.model;

import lombok.Data;
import java.util.Map;

@Data
public class PaymentProcessRequest {
    private Integer bookingId;
    private Double amount;
    private String currency;
    private String paymentMethod;
    private Map<String, String> paymentDetails;
}