package com.backend_project.hotel.controller;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class BookingController {

    private static final Logger LOGGER = Logger.getLogger(BookingController.class.getName());

    @Autowired
    private BookingService bookingService;

    @PostMapping("/")
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest bookingRequest,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        LOGGER.info("Creating booking for customer " + customerId + " with payload: " + bookingRequest);
        try {
            if (bookingRequest.getRoomId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Room ID is required"));
            }
            if (bookingRequest.getGuests() <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Number of guests must be greater than 0"));
            }

            LocalDate checkIn = LocalDate.parse(bookingRequest.getCheckInDate());
            LocalDate checkOut = LocalDate.parse(bookingRequest.getCheckOutDate());

            if (checkIn.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Check-in date cannot be in the past"));
            }

            if (!checkOut.isAfter(checkIn)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Check-out date must be after check-in date"));
            }

            ResponseEntity<?> response = bookingService.createBooking(bookingRequest, customerId);
            LOGGER.info("Booking created successfully: " + response.getBody());
            return response;
        } catch (DateTimeParseException e) {
            LOGGER.warning("Invalid date format: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid date format. Use YYYY-MM-DD"));
        } catch (Exception e) {
            LOGGER.severe("Error creating booking: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to create booking: " + e.getMessage()));
        }
    }

    @GetMapping("/customer")
    public ResponseEntity<List<BookingModel>> getCustomerBookings(
            @RequestHeader("X-Customer-Id") Integer customerId) {
        try {
            LOGGER.info("Fetching bookings for customer " + customerId);
            List<BookingModel> bookings = bookingService.getCustomerBookings(customerId);
            LOGGER.info("Returning " + bookings.size() + " bookings for customer " + customerId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching bookings for customer " + customerId + ": " + e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/{bookingId}/details")
    public ResponseEntity<?> getBookingDetailsWithPayments(
            @PathVariable Integer bookingId,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        try {
            BookingDetailsResponse response = bookingService.getBookingDetailsWithPayments(bookingId, customerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.severe("Error fetching booking details: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to get booking details: " + e.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer bookingId,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        try {
            ResponseEntity<?> response = bookingService.cancelBooking(bookingId, customerId);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error cancelling booking: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to cancel booking: " + e.getMessage()));
        }
    }
}