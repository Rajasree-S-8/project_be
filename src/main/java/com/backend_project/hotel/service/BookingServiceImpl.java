package com.backend_project.hotel.service;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
            CustomerModel customer = customerRepository.findById(customerId)
                    .orElse(null);
            if (customer == null) {
                LOGGER.warning("Customer not found: " + customerId);
                return ResponseEntity.status(401)
                        .body(Map.of("message", "User not logged in or invalid customer ID"));
            }

            RoomModel room = roomRepository.findById(bookingRequest.getRoomId())
                    .orElse(null);
            if (room == null) {
                LOGGER.warning("Room not found: " + bookingRequest.getRoomId());
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Room not found"));
            }

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

            if (!isRoomAvailable(room.getRoomId(), checkInDate, checkOutDate)) {
                LOGGER.warning("Room " + room.getRoomId() + " not available for dates " + checkInDate + " to " + checkOutDate);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Room not available for selected dates"));
            }

            long days = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (days <= 0) {
                LOGGER.warning("Invalid date range: " + checkInDate + " to " + checkOutDate);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Check-out date must be after check-in date"));
            }
            double totalPrice = room.getPrice() * days;

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
            
            List<BookingModel> simplifiedBookings = new ArrayList<>();
            for (BookingModel booking : bookings) {
                BookingModel simplified = new BookingModel();
                simplified.setBookingId(booking.getBookingId());
                
                RoomModel room = booking.getRoom();
                RoomModel simpleRoom = new RoomModel();
                simpleRoom.setRoomId(room.getRoomId());
                simpleRoom.setRoomNumber(room.getRoomNumber());
                simpleRoom.setRoomType(room.getRoomType());
                simpleRoom.setPrice(room.getPrice());
                simpleRoom.setAcType(room.getAcType());
                simplified.setRoom(simpleRoom);
                
                simplified.setCheckInDate(booking.getCheckInDate());
                simplified.setCheckOutDate(booking.getCheckOutDate());
                simplified.setGuests(booking.getGuests());
                simplified.setTotalPrice(booking.getTotalPrice());
                simplified.setStatus(booking.getStatus());
                simplified.setBookingDate(booking.getBookingDate());
                
                simplifiedBookings.add(simplified);
            }
            return simplifiedBookings;
        } catch (Exception e) {
            LOGGER.severe("Error fetching bookings for customer " + customerId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
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
            
            if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Booking is already cancelled"));
            }
            
            if (booking.getCheckInDate().isBefore(LocalDate.now())) {
                LOGGER.warning("Cannot cancel booking after check-in date: " + bookingId);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Cannot cancel booking after check-in date"));
            }
            
            booking.setStatus("cancelled");
            RoomModel room = booking.getRoom();
            room.setIsAvailable(true);
            roomRepository.save(room);
            bookingRepository.save(booking);
            
            LOGGER.info("Booking cancelled successfully: " + bookingId);
            
            boolean hasPayment = false;
            double refundAmount = 0;
            
            if (booking.getPayments() != null && !booking.getPayments().isEmpty()) {
                for (PaymentModel payment : booking.getPayments()) {
                    if ("completed".equalsIgnoreCase(payment.getStatus())) {
                        hasPayment = true;
                        break;
                    }
                }
                
                if (hasPayment) {
                    refundAmount = booking.getTotalPrice() * 0.8;
                    return ResponseEntity.ok(Map.of(
                        "message", "Booking cancelled successfully. Refund of 80% will be processed.",
                        "refundAmount", refundAmount,
                        "refundNote", "Refund will be processed to your original payment method within 5-7 business days."
                    ));
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Booking cancelled successfully",
                "refundAmount", 0
            ));
            
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
            
            BookingModel simplified = new BookingModel();
            simplified.setBookingId(booking.getBookingId());
            
            RoomModel room = booking.getRoom();
            RoomModel simpleRoom = new RoomModel();
            simpleRoom.setRoomId(room.getRoomId());
            simpleRoom.setRoomNumber(room.getRoomNumber());
            simpleRoom.setRoomType(room.getRoomType());
            simpleRoom.setPrice(room.getPrice());
            simpleRoom.setAcType(room.getAcType());
            simplified.setRoom(simpleRoom);
            
            CustomerModel customer = booking.getCustomer();
            CustomerModel simpleCustomer = new CustomerModel();
            simpleCustomer.setUserId(customer.getUserId());
            simpleCustomer.setFullName(customer.getFullName());
            simpleCustomer.setEmail(customer.getEmail());
            simpleCustomer.setPhoneNumber(customer.getPhoneNumber());
            simpleCustomer.setAddress(customer.getAddress());
            simplified.setCustomer(simpleCustomer);
            
            simplified.setCheckInDate(booking.getCheckInDate());
            simplified.setCheckOutDate(booking.getCheckOutDate());
            simplified.setGuests(booking.getGuests());
            simplified.setTotalPrice(booking.getTotalPrice());
            simplified.setStatus(booking.getStatus());
            simplified.setBookingDate(booking.getBookingDate());
            
            return simplified;
        } catch (Exception e) {
            LOGGER.severe("Error fetching booking details: " + e.getMessage());
            return null;
        }
    }

    @Override
    public BookingDetailsResponse getBookingDetailsWithPayments(Integer bookingId, Integer customerId) {
        try {
            BookingModel booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!booking.getCustomer().getUserId().equals(customerId)) {
                throw new RuntimeException("Unauthorized access to booking details");
            }

            BookingDetailsResponse response = new BookingDetailsResponse();
            response.setBookingId(booking.getBookingId());
            
            // Room info
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", booking.getRoom().getRoomId());
            roomInfo.put("roomNumber", booking.getRoom().getRoomNumber());
            roomInfo.put("roomType", booking.getRoom().getRoomType());
            roomInfo.put("price", booking.getRoom().getPrice());
            roomInfo.put("acType", booking.getRoom().getAcType());
            roomInfo.put("imageUrl", booking.getRoom().getImageUrl());
            response.setRoom(roomInfo);
            
            // Customer info
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("userId", booking.getCustomer().getUserId());
            customerInfo.put("name", booking.getCustomer().getFullName());
            customerInfo.put("email", booking.getCustomer().getEmail());
            customerInfo.put("phoneNumber", booking.getCustomer().getPhoneNumber());
            customerInfo.put("address", booking.getCustomer().getAddress());
            response.setCustomer(customerInfo);
            
            response.setCheckInDate(booking.getCheckInDate());
            response.setCheckOutDate(booking.getCheckOutDate());
            response.setGuests(booking.getGuests());
            response.setTotalPrice(booking.getTotalPrice());
            response.setStatus(booking.getStatus());
            response.setBookingDate(booking.getBookingDate());
            
            // Payments
            if (booking.getPayments() != null && !booking.getPayments().isEmpty()) {
                List<Map<String, Object>> paymentsList = new ArrayList<>();
                for (PaymentModel payment : booking.getPayments()) {
                    Map<String, Object> paymentMap = new HashMap<>();
                    paymentMap.put("paymentId", payment.getPaymentId());
                    paymentMap.put("amount", payment.getAmount());
                    paymentMap.put("currency", payment.getCurrency());
                    paymentMap.put("paymentMethod", payment.getPaymentMethod());
                    paymentMap.put("status", payment.getStatus());
                    paymentMap.put("paymentDate", payment.getPaymentDate());
                    paymentMap.put("transactionId", payment.getTransactionId());
                    paymentMap.put("cardBrand", payment.getCardBrand());
                    paymentMap.put("cardLastFour", payment.getCardLastFour());
                    paymentsList.add(paymentMap);
                }
                response.setPayments(paymentsList);
            }
            
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
            return paymentService.processDirectPayment(bookingId, customerId, amount, currency, 
                                                     paymentMethod, paymentDetails);
        } catch (Exception e) {
            LOGGER.severe("Error processing payment for booking " + bookingId + ": " + e.getMessage());
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }
}