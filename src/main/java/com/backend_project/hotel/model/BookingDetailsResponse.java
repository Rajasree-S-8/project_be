package com.backend_project.hotel.model;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BookingDetailsResponse {
    private Integer bookingId;
    private RoomModel room;
    private CustomerModel customer;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guests;
    private Double totalPrice;
    private String status;
    private LocalDate bookingDate;
    private List<PaymentModel> payments;
}