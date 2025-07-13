package com.backend_project.hotel.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "bookings")
public class BookingModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookingId;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private RoomModel room;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerModel customer;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private Integer guests;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private String status; // "pending", "confirmed", "cancelled"

    @Column(nullable = false)
    private LocalDate bookingDate;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<PaymentModel> payments;
}