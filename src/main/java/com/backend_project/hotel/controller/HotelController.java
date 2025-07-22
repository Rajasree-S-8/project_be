package com.backend_project.hotel.controller;

import com.backend_project.hotel.model.HotelModel;
import com.backend_project.hotel.service.FileStorageService;
import com.backend_project.hotel.service.HotelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "http://localhost:3000")
public class HotelController {

    private static final Logger logger = LoggerFactory.getLogger(HotelController.class);

    @Autowired
    private HotelService hotelService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/pdf/{id}")
    public ResponseEntity<?> downloadStaffPdf(@PathVariable Integer id) {
        try {
            logger.info("Attempting to generate PDF for staff ID: {}", id);
            
            byte[] pdfContent = hotelService.generateStaffPdf(id);
            
            if (pdfContent == null || pdfContent.length == 0) {
                logger.error("Generated PDF content is empty for staff ID: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to generate PDF: Empty content");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("staff_profile_" + id + ".pdf")
                    .build());
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            logger.info("Successfully generated PDF for staff ID: {}", id);
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
            
        } catch (DocumentException e) {
            logger.error("PDF generation error for staff ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("PDF generation error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Staff not found for ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Staff not found: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error generating PDF for staff ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate PDF: " + e.getMessage());
        }
    }

    @GetMapping("/pdf/all")
    public ResponseEntity<?> downloadAllStaffPdf() {
        try {
            logger.info("Attempting to generate PDF for all staff");
            
            byte[] pdfContent = hotelService.generateAllStaffPdf();
            
            if (pdfContent == null || pdfContent.length == 0) {
                logger.error("Generated PDF content is empty for all staff report");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to generate PDF: Empty content");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("staff_report_" + System.currentTimeMillis() + ".pdf")
                    .build());
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            logger.info("Successfully generated PDF for all staff");
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
            
        } catch (DocumentException e) {
            logger.error("PDF generation error for all staff: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("PDF generation error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("No staff found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No staff members found");
        } catch (Exception e) {
            logger.error("Unexpected error generating all staff PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate PDF: " + e.getMessage());
        }
    }

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
            logger.error("Error adding staff: {}", e.getMessage());
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
            logger.error("Error updating staff with ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Integer id) {
        return hotelService.deleteStaff(id);
    }
}