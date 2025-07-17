package com.backend_project.hotel.controller;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.backend_project.hotel.model.HotelModel;
import com.backend_project.hotel.service.FileStorageService;
import com.backend_project.hotel.service.HotelService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "http://localhost:3000")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody HotelModel loginRequest) {
        return hotelService.login(loginRequest.getUsername(), loginRequest.getPassword());
    }

    @PostMapping("/restaurant-login")
    public ResponseEntity<?> restaurantLogin(@RequestBody HotelModel loginRequest) {
        return hotelService.restaurantLogin(loginRequest.getUsername(), loginRequest.getPassword());
    }

    @PostMapping(value = "/add", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<HotelModel> addStaff(
            @RequestPart("staff") HotelModel model,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            if (image != null && !image.isEmpty()) {
                String imageFilename = fileStorageService.storeFile(image);
                model.setImage(imageFilename);
            }
            return hotelService.addStaff(model);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<HotelModel>> getAllStaff() {
        return hotelService.getAllStaff();
    }

    @PutMapping(value = "/update/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<HotelModel> updateStaff(
            @PathVariable Integer id,
            @RequestPart("staff") String staffJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            HotelModel model = objectMapper.readValue(staffJson, HotelModel.class);
            
            if (image != null && !image.isEmpty()) {
                String imageFilename = fileStorageService.storeFile(image);
                model.setImage(imageFilename);
            }
            return hotelService.updateStaff(id, model);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Integer id) {
        return hotelService.deleteStaff(id);
    }
}