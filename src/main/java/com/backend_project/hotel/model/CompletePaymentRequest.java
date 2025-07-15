package com.backend_project.hotel.model;

import lombok.Data;
import java.util.Map;

@Data
public class CompletePaymentRequest {
    private Integer bookingId;
    private Map<String, String> paymentDetails;
}