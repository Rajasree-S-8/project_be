package com.backend_project.hotel.service;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    @Transactional
    public PaymentModel processDirectPayment(Integer bookingId, Integer customerId, 
                                            Double amount, String currency,
                                            String paymentMethod, Map<String, String> paymentDetails) {
        BookingModel booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        CustomerModel customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Unauthorized payment attempt");
        }

        if (!booking.getStatus().equals("pending")) {
            throw new RuntimeException("Booking is not in a payable state");
        }

        // Validate payment details
        if (paymentDetails == null || 
            !paymentDetails.containsKey("cardNumber") || 
            !paymentDetails.containsKey("expiry") || 
            !paymentDetails.containsKey("cvv") || 
            !paymentDetails.containsKey("name")) {
            throw new RuntimeException("Incomplete payment details");
        }

        // Simulate payment processing
        String cardNumber = paymentDetails.get("cardNumber");
        String cardLastFour = cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : cardNumber;

        // Create payment record
        PaymentModel payment = new PaymentModel();
        payment.setBooking(booking);
        payment.setCustomer(customer);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus("completed");
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString());
        payment.setPaymentMethod(paymentMethod);
        payment.setCardLastFour(cardLastFour);
        payment.setCardBrand(determineCardBrand(cardNumber));

        // Update booking status
        booking.setStatus("confirmed");
        bookingRepository.save(booking);

        return paymentRepository.save(payment);
    }

    private String determineCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) return "Visa";
        if (cardNumber.startsWith("5")) return "Mastercard";
        return "Unknown";
    }

    @Override
    public String createPaymentIntent(Double amount, String currency, Integer customerId, Integer bookingId) {
        return "TXN-" + UUID.randomUUID().toString();
    }

    @Override
    public void handlePaymentWebhook(Map<String, Object> payload, String sigHeader) {
        // Not used in current frontend
    }

    @Override
    public PaymentModel getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}