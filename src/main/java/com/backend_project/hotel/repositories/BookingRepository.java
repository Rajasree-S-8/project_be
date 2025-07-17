package com.backend_project.hotel.repositories;

import com.backend_project.hotel.model.BookingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingModel, Integer> {
    List<BookingModel> findByCustomerUserId(Integer customerId);

    @Query("SELECT b FROM BookingModel b WHERE b.room.roomId = :roomId " +
           "AND b.status != 'cancelled' " +
           "AND (b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn)")
    List<BookingModel> findConflictingBookings(Integer roomId, LocalDate checkIn, LocalDate checkOut);

    
}