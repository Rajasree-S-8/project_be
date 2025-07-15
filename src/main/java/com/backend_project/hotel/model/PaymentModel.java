package com.backend_project.hotel.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class PaymentModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    
    private BookingModel booking;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerModel customer;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status; // "pending", "completed", "failed"

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(unique = true)
    private String transactionId;

    private String paymentMethod; // "credit_card", "debit_card"
    private String cardType; // "credit", "debit"
    private String cardLastFour;
    private String cardBrand;
    private String receiptUrl;
    private String failureMessage;
}