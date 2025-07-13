package com.backend_project.hotel.service;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger LOGGER = Logger.getLogger(BookingServiceImpl.class.getName());

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    @Transactional
    public ResponseEntity<?> createBooking(BookingRequest bookingRequest, Integer customerId) {
        LOGGER.info("Creating booking for customer " + customerId + ", room " + bookingRequest.getRoomId());
        try {
            // Validate customer
            CustomerModel customer = customerRepository.findById(customerId)
                    .orElse(null);
            if (customer == null) {
                LOGGER.warning("Customer not found: " + customerId);
                return ResponseEntity.status(401)
                        .body(Map.of("message", "User not logged in or invalid customer ID"));
            }

            // Validate room
            RoomModel room = roomRepository.findById(bookingRequest.getRoomId())
                    .orElse(null);
            if (room == null) {
                LOGGER.warning("Room not found: " + bookingRequest.getRoomId());
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Room not found"));
            }

            // Parse dates
            LocalDate checkInDate;
            LocalDate checkOutDate;
            try {
                checkInDate = LocalDate.parse(bookingRequest.getCheckInDate());
                checkOutDate = LocalDate.parse(bookingRequest.getCheckOutDate());
            } catch (Exception e) {
                LOGGER.warning("Invalid date format: " + e.getMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid date format. Use YYYY-MM-DD"));
            }

            // Check availability
            if (!isRoomAvailable(room.getRoomId(), checkInDate, checkOutDate)) {
                LOGGER.warning("Room " + room.getRoomId() + " not available for dates " + checkInDate + " to " + checkOutDate);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Room not available for selected dates"));
            }

            // Calculate total price
            long days = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (days <= 0) {
                LOGGER.warning("Invalid date range: " + checkInDate + " to " + checkOutDate);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Check-out date must be after check-in date"));
            }
            double totalPrice = room.getPrice() * days;

            // Create and save booking
            BookingModel booking = new BookingModel();
            booking.setRoom(room);
            booking.setCustomer(customer);
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);
            booking.setGuests(bookingRequest.getGuests());
            booking.setTotalPrice(totalPrice);
            booking.setStatus("pending");
            booking.setBookingDate(LocalDate.now());

            BookingModel savedBooking = bookingRepository.save(booking);
            room.setIsAvailable(false);
            roomRepository.save(room);
            LOGGER.info("Booking saved successfully: " + savedBooking.getBookingId());
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Booking created successfully",
                    "bookingId", savedBooking.getBookingId()
            ));
        } catch (Exception e) {
            LOGGER.severe("Error creating booking: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to create booking: " + e.getMessage()));
        }
    }

    private boolean isRoomAvailable(Integer roomId, LocalDate checkIn, LocalDate checkOut) {
        List<BookingModel> conflicts = bookingRepository.findConflictingBookings(
                roomId, checkIn, checkOut);
        return conflicts.isEmpty();
    }

    @Override
    public List<BookingModel> getCustomerBookings(Integer customerId) {
        try {
            if (!customerRepository.existsById(customerId)) {
                LOGGER.warning("Customer not found for ID: " + customerId);
                return Collections.emptyList();
            }
            List<BookingModel> bookings = bookingRepository.findByCustomerUserId(customerId);
            return bookings != null ? bookings : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.severe("Error fetching bookings for customer " + customerId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ResponseEntity<?> cancelBooking(Integer bookingId, Integer customerId) {
        try {
            BookingModel booking = bookingRepository.findById(bookingId)
                    .orElse(null);
            if (booking == null) {
                LOGGER.warning("Booking not found: " + bookingId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Booking not found"));
            }
            if (!booking.getCustomer().getUserId().equals(customerId)) {
                LOGGER.warning("Unauthorized attempt to cancel booking " + bookingId + " by customer " + customerId);
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Unauthorized to cancel this booking"));
            }
            if (!booking.getStatus().equals("pending")) {
                LOGGER.warning("Cannot cancel non-pending booking: " + bookingId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Only pending bookings can be cancelled"));
            }
            booking.setStatus("cancelled");
            RoomModel room = booking.getRoom();
            room.setIsAvailable(true);
            roomRepository.save(room);
            bookingRepository.save(booking);
            LOGGER.info("Booking cancelled successfully: " + bookingId);
            return ResponseEntity.ok(Map.of("message", "Booking cancelled successfully"));
        } catch (Exception e) {
            LOGGER.severe("Error cancelling booking: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to cancel booking: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<?> checkRoomAvailability(Integer roomId, String checkIn, String checkOut) {
        try {
            LocalDate checkInDate = LocalDate.parse(checkIn);
            LocalDate checkOutDate = LocalDate.parse(checkOut);
            boolean isAvailable = isRoomAvailable(roomId, checkInDate, checkOutDate);
            return ResponseEntity.ok(Map.of("isAvailable", isAvailable));
        } catch (Exception e) {
            LOGGER.severe("Error checking availability: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to check availability: " + e.getMessage()));
        }
    }

    @Override
    public BookingModel getBookingDetails(Integer bookingId, Integer customerId) {
        try {
            BookingModel booking = bookingRepository.findById(bookingId)
                    .orElse(null);
            if (booking == null) {
                LOGGER.warning("Booking not found: " + bookingId);
                return null;
            }
            if (!booking.getCustomer().getUserId().equals(customerId)) {
                LOGGER.warning("Unauthorized access to booking " + bookingId + " by customer " + customerId);
                return null;
            }
            return booking;
        } catch (Exception e) {
            LOGGER.severe("Error fetching booking details: " + e.getMessage());
            return null;
        }
    }

    @Override
    public BookingDetailsResponse getBookingDetailsWithPayments(Integer bookingId, Integer customerId) {
        try {
            BookingModel booking = bookingRepository.findById(bookingId)
                    .orElse(null);
            if (booking == null) {
                LOGGER.warning("Booking not found: " + bookingId);
                throw new RuntimeException("Booking not found");
            }
            if (!booking.getCustomer().getUserId().equals(customerId)) {
                LOGGER.warning("Unauthorized access to booking " + bookingId + " by customer " + customerId);
                throw new RuntimeException("Unauthorized access to booking details");
            }
            BookingDetailsResponse response = new BookingDetailsResponse();
            response.setBookingId(booking.getBookingId());
            response.setRoom(booking.getRoom());
            response.setCustomer(booking.getCustomer());
            response.setCheckInDate(booking.getCheckInDate());
            response.setCheckOutDate(booking.getCheckOutDate());
            response.setGuests(booking.getGuests());
            response.setTotalPrice(booking.getTotalPrice());
            response.setStatus(booking.getStatus());
            response.setBookingDate(booking.getBookingDate());
            response.setPayments(booking.getPayments());
            return response;
        } catch (Exception e) {
            LOGGER.severe("Error fetching booking details with payments: " + e.getMessage());
            throw new RuntimeException("Failed to fetch booking details: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentModel processPayment(Integer bookingId, Integer customerId, Double amount, String currency,
                                      String paymentMethod, Map<String, String> paymentDetails) {
        try {
            BookingModel booking = bookingRepository.findById(bookingId)
                    .orElse(null);
            if (booking == null) {
                LOGGER.warning("Booking not found: " + bookingId);
                throw new RuntimeException("Booking not found");
            }
            if (!booking.getCustomer().getUserId().equals(customerId)) {
                LOGGER.warning("Unauthorized payment attempt for booking " + bookingId + " by customer " + customerId);
                throw new RuntimeException("Unauthorized payment attempt");
            }
            CustomerModel customer = customerRepository.findById(customerId)
                    .orElse(null);
            if (customer == null) {
                LOGGER.warning("Customer not found: " + customerId);
                throw new RuntimeException("Customer not found");
            }
            PaymentModel payment = new PaymentModel();
            payment.setBooking(booking);
            payment.setCustomer(customer);
            payment.setAmount(amount);
            payment.setCurrency(currency);
            payment.setPaymentMethod(paymentMethod);
            payment.setStatus("completed");
            payment.setPaymentDate(LocalDateTime.now());
            payment.setTransactionId(UUID.randomUUID().toString());
            payment.setCardType(paymentDetails.get("cardType"));
            payment.setCardLastFour(paymentDetails.get("cardNumber").substring(paymentDetails.get("cardNumber").length() - 4));
            payment.setCardBrand(paymentDetails.get("cardBrand"));
            PaymentModel savedPayment = paymentRepository.save(payment);
            booking.setStatus("confirmed");
            bookingRepository.save(booking);
            LOGGER.info("Payment processed successfully for booking " + bookingId);
            return savedPayment;
        } catch (Exception e) {
            LOGGER.severe("Error processing payment for booking " + bookingId + ": " + e.getMessage());
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }
}