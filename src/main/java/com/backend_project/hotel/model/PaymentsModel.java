package com.backend_project.hotel.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paymentss")
public class PaymentsModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private OrderModel order;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status; // "pending", "completed", "failed"

    @Column
    private String paymentMethod;

    @Column
    private String transactionId;

    @Column(nullable = false)
    private LocalDateTime paymentDate;
}