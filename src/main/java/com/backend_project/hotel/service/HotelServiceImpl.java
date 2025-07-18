package com.backend_project.hotel.service;

import java.util.List;
import java.io.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.backend_project.hotel.model.HotelModel;
import com.backend_project.hotel.repositories.HotelRepositories;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class HotelServiceImpl implements HotelService {

    @Autowired
    private HotelRepositories hotelRepositories;

    @Autowired
    private FileStorageService fileStorageService;
    
    // Add PDF font constants
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font CONTENT_FONT = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8);

    // ... (keep all your existing methods)

    @Override
    public ResponseEntity<byte[]> getStaffPdf(Integer id) {
        return hotelRepositories.findById(id)
                .map(staff -> {
                    try {
                        byte[] pdfBytes = generateStaffPdf(staff);
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=staff_" + id + ".pdf")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdfBytes);
                    } catch (DocumentException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private byte[] generateStaffPdf(HotelModel staff) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Add hotel header
        addHotelHeader(document);

        // Add title
        Paragraph title = new Paragraph("STAFF DETAILS", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);

        // Add staff information
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);

        addSectionHeader(table, "STAFF INFORMATION", 2);
        addTableRow(table, "Staff ID:", staff.getId().toString());
        addTableRow(table, "Username:", staff.getUsername());
        addTableRow(table, "Full Name:", staff.getFullname());
        addTableRow(table, "Email:", staff.getEmail());
        addTableRow(table, "Address:", staff.getAddress());
        addTableRow(table, "Age:", String.valueOf(staff.getAge()));
        addTableRow(table, "Phone Number:", staff.getPhonenumber());
        addTableRow(table, "Role:", staff.getRole());
        
        document.add(table);

        // Add footer
        addFooter(document);

        document.close();
        return outputStream.toByteArray();
    }

    private void addHotelHeader(Document document) throws DocumentException {
        Paragraph hotelName = new Paragraph("Revuzz Hotel", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD));
        hotelName.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph address = new Paragraph("Kollam, Kerala, 691572,Thiruvanathapuram", CONTENT_FONT);
        address.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph phone = new Paragraph("Phone: +91 9876543210 | Email: revuzz@hotel.com", SMALL_FONT);
        phone.setAlignment(Element.ALIGN_CENTER);
        
        document.add(hotelName);
        document.add(address);
        document.add(phone);
        document.add(new Paragraph(" ")); // Add some space
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20f);
        
        Paragraph thankYou = new Paragraph("This is an official staff record", new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC));
        thankYou.setAlignment(Element.ALIGN_CENTER);
        
        footer.add(thankYou);
        document.add(footer);
    }

    private void addSectionHeader(PdfPTable table, String title, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(title, HEADER_FONT));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new BaseColor(220, 220, 220));
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, CONTENT_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }
}