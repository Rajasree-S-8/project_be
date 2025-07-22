package com.backend_project.hotel.service;

import com.backend_project.hotel.model.HotelModel;
import com.itextpdf.text.DocumentException;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface HotelService {
    ResponseEntity<HotelModel> addStaff(HotelModel model);
    ResponseEntity<List<HotelModel>> getAllStaff();
    ResponseEntity<HotelModel> updateStaff(Integer id, HotelModel model);
    ResponseEntity<Void> deleteStaff(Integer id);
    ResponseEntity<?> login(String username, String password);
    ResponseEntity<?> restaurantLogin(String username, String password);
    byte[] generateStaffPdf(Integer id) throws DocumentException;
    byte[] generateAllStaffPdf() throws DocumentException;
}