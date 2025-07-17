package com.backend_project.hotel.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.backend_project.hotel.model.HotelModel;

public interface HotelService {
    ResponseEntity<HotelModel> addStaff(HotelModel model);
    ResponseEntity<List<HotelModel>> getAllStaff();
    ResponseEntity<HotelModel> updateStaff(Integer id, HotelModel model);
    ResponseEntity<Void> deleteStaff(Integer id);
    ResponseEntity<?> login(String username, String password);
    ResponseEntity<?> restaurantLogin(String username, String password);
}