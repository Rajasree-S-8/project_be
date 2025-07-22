package com.backend_project.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "staff")
public class HotelModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "staff_username")
    private String username;

    @Column(name = "staff_fullname")
    private String fullname;

    @Column(name = "staff_email")
    private String email;

    @Column(name = "staff_address")
    private String address;

    @Column(name = "staff_date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "staff_phonenumber")
    private String phonenumber;

    @Column(name = "staff_password")
    private String password;

    @Column(name = "staff_role")
    private String role;

    @Column(name = "staff_image")
    private String image;

    @Column(name = "staff_joining_date")
    private LocalDate joiningDate;

    @Column(name = "staff_experience")
    private Integer experience;

    @Column(name = "staff_qualification")
    private String qualification;
    
    @Column(name = "staff_age") // Added age field
    private Integer age;
}