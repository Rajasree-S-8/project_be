package com.backend_project.hotel.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
public class CustomerModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "password")
    private String password;

    @Transient
    private String confirmPassword;

    @Lob
    @Column(name = "image")
    private String image;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<OrderModel> orders;
    
    @OneToMany(mappedBy = "customer")
    private List<BookingModel> bookings;
}