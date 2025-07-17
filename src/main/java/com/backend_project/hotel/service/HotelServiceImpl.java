package com.backend_project.hotel.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.backend_project.hotel.model.HotelModel;
import com.backend_project.hotel.repositories.HotelRepositories;

@Service
public class HotelServiceImpl implements HotelService {

    @Autowired
    private HotelRepositories hotelRepositories;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public ResponseEntity<HotelModel> addStaff(HotelModel staff) {
        HotelModel savedStaff = hotelRepositories.save(staff);
        return ResponseEntity.ok(savedStaff);
    }

    @Override
    public ResponseEntity<List<HotelModel>> getAllStaff() {
        List<HotelModel> staffList = hotelRepositories.findAll();
        return ResponseEntity.ok(staffList);
    }

    @Override
    public ResponseEntity<HotelModel> updateStaff(Integer id, HotelModel model) {
        return hotelRepositories.findById(id)
                .map(staff -> {
                    staff.setUsername(model.getUsername());
                    staff.setFullname(model.getFullname());
                    staff.setEmail(model.getEmail());
                    staff.setAddress(model.getAddress());
                    staff.setAge(model.getAge());
                    staff.setPhonenumber(model.getPhonenumber());
                    staff.setRole(model.getRole());
                    if (model.getPassword() != null && !model.getPassword().isEmpty()) {
                        staff.setPassword(model.getPassword());
                    }
                    if (model.getImage() != null && !model.getImage().isEmpty()) {
                        staff.setImage(model.getImage());
                    }
                    return ResponseEntity.ok(hotelRepositories.save(staff));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteStaff(Integer id) {
        return hotelRepositories.findById(id)
                .map(staff -> {
                    hotelRepositories.delete(staff);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<?> login(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        return hotelRepositories.findByUsernameAndPassword(username, password)
                .map(staff -> {
                    if (!"Hotel Manager".equals(staff.getRole())) {
                        return ResponseEntity.badRequest().body("Only Hotel Managers can log in");
                    }
                    return ResponseEntity.ok().body(staff);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid username or password"));
    }

    @Override
    public ResponseEntity<?> restaurantLogin(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        return hotelRepositories.findByUsernameAndPassword(username, password)
                .map(staff -> {
                    if (!"Restaurant Manager".equals(staff.getRole())) {
                        return ResponseEntity.badRequest().body("Only Restaurant Managers can log in");
                    }
                    return ResponseEntity.ok().body(staff);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid username or password"));
    }
}