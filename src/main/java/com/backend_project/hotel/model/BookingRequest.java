package com.backend_project.hotel.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    private Integer roomId;
    private String checkInDate;
    private String checkOutDate;
    private Integer guests;
}