package com.backend_project.hotel.model;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class BookingDetailsResponse {
    private Integer bookingId;
    private Map<String, Object> room;
    private Map<String, Object> customer;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guests;
    private Double totalPrice;
    private String status;
    private LocalDate bookingDate;
    private List<Map<String, Object>> payments;
}