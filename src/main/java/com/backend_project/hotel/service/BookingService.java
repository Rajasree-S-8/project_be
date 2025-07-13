package com.backend_project.hotel.service;

import com.backend_project.hotel.model.BookingModel;
import com.backend_project.hotel.model.BookingRequest;
import com.backend_project.hotel.model.BookingDetailsResponse;
import com.backend_project.hotel.model.PaymentModel;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

public interface BookingService {
    ResponseEntity<?> createBooking(BookingRequest bookingRequest, Integer customerId);
    List<BookingModel> getCustomerBookings(Integer customerId);
    ResponseEntity<?> cancelBooking(Integer bookingId, Integer customerId);
    ResponseEntity<?> checkRoomAvailability(Integer roomId, String checkIn, String checkOut);
    BookingModel getBookingDetails(Integer bookingId, Integer customerId);
    BookingDetailsResponse getBookingDetailsWithPayments(Integer bookingId, Integer customerId);
    PaymentModel processPayment(Integer bookingId, Integer customerId, Double amount, String currency,
                               String paymentMethod, Map<String, String> paymentDetails);
}