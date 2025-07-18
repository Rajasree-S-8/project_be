package com.backend_project.hotel.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.backend_project.hotel.model.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import com.itextpdf.text.pdf.draw.LineSeparator;

@Service
public class PdfService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font CONTENT_FONT = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8);

    public byte[] generateBookingPdf(BookingDetailsResponse bookingDetails) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Add hotel logo and header
        addHotelHeader(document);

        // Add title
        Paragraph title = new Paragraph("BOOKING CONFIRMATION", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);

        // Add booking summary
        addBookingSummary(document, bookingDetails);

        // Add customer information
        addCustomerInfo(document, bookingDetails.getCustomer());

        // Add room information
        addRoomInfo(document, bookingDetails.getRoom());

        // Add payment information if exists
        if (bookingDetails.getPayments() != null && !bookingDetails.getPayments().isEmpty()) {
            addPaymentInfo(document, bookingDetails.getPayments());
        }

        // Add terms and conditions
        addTermsAndConditions(document);

        // Add footer
        addFooter(document);

        document.close();
        return outputStream.toByteArray();
    }

    private void addHotelHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        
        // Hotel name
        Paragraph hotelName = new Paragraph("Revuzz Hotel", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD));
        hotelName.setAlignment(Element.ALIGN_CENTER);
        
        // Address
        Paragraph address = new Paragraph("Kollam, Kerala, 691572,Thiruvanathapuram", CONTENT_FONT);
        address.setAlignment(Element.ALIGN_CENTER);
        
        // Phone
        Paragraph phone = new Paragraph("Phone: +91 9876543210 | Email: revuzz@hotel.com", SMALL_FONT);
        phone.setAlignment(Element.ALIGN_CENTER);
        
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(hotelName);
        cell.addElement(address);
        cell.addElement(phone);
        cell.setPaddingBottom(10f);
        
        headerTable.addCell(cell);
        document.add(headerTable);
        
        // Add separator line
        document.add(new Chunk(new LineSeparator()));
    }

    private void addBookingSummary(Document document, BookingDetailsResponse booking) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);
        
        // Section header
        addSectionHeader(table, "BOOKING SUMMARY", 2);
        
        // Booking details
        addTableRow(table, "Booking ID:", booking.getBookingId().toString());
        addTableRow(table, "Booking Date:", booking.getBookingDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        addTableRow(table, "Booking Status:", booking.getStatus().toUpperCase());
        addTableRow(table, "Check-in Date:", booking.getCheckInDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        addTableRow(table, "Check-out Date:", booking.getCheckOutDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        addTableRow(table, "Number of Nights:", 
            String.valueOf(booking.getCheckInDate().until(booking.getCheckOutDate()).getDays()));
        addTableRow(table, "Number of Guests:", String.valueOf(booking.getGuests()));
        addTableRow(table, "Total Amount:", String.format("₹%.2f", booking.getTotalPrice()));
        
        document.add(table);
    }

    private void addCustomerInfo(Document document, Map<String, Object> customer) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);
        
        // Section header
        addSectionHeader(table, "CUSTOMER INFORMATION", 2);
        
        // Customer details
        addTableRow(table, "Customer ID:", customer.get("userId").toString());
        addTableRow(table, "Full Name:", customer.get("name").toString());
        addTableRow(table, "Email:", customer.get("email").toString());
        if (customer.containsKey("phoneNumber")) {
            addTableRow(table, "Phone:", customer.get("phoneNumber").toString());
        }
        if (customer.containsKey("address")) {
            addTableRow(table, "Address:", customer.get("address").toString());
        }
        
        document.add(table);
    }

    private void addRoomInfo(Document document, Map<String, Object> room) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);
        
        // Section header
        addSectionHeader(table, "ROOM INFORMATION", 2);
        
        // Room details
        addTableRow(table, "Room Number:", room.get("roomNumber").toString());
        addTableRow(table, "Room Type:", room.get("roomType").toString());
        addTableRow(table, "Price Per Night:", String.format("₹%.2f", room.get("price")));
        if (room.containsKey("acType")) {
            addTableRow(table, "AC Type:", room.get("acType").toString());
        }
        if (room.containsKey("capacity")) {
            addTableRow(table, "Capacity:", room.get("capacity").toString());
        }
        if (room.containsKey("bedType")) {
            addTableRow(table, "Bed Type:", room.get("bedType").toString());
        }
        
        document.add(table);
    }

    private void addPaymentInfo(Document document, List<Map<String, Object>> payments) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(15f);
        table.setWidths(new float[]{1, 2, 2, 2, 2});
        
        // Section header
        addSectionHeader(table, "PAYMENT HISTORY", 5);
        
        // Column headers
        String[] headers = {"#", "Transaction ID", "Amount", "Payment Method", "Date"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            cell.setPadding(5f);
            table.addCell(cell);
        }
        
        // Payment rows
        int counter = 1;
        for (Map<String, Object> payment : payments) {
            table.addCell(new Phrase(String.valueOf(counter++), CONTENT_FONT));
            table.addCell(new Phrase(payment.get("transactionId").toString(), CONTENT_FONT));
            table.addCell(new Phrase(String.format("₹%.2f", payment.get("amount")), CONTENT_FONT));
            
            String paymentMethod = payment.get("paymentMethod").toString();
            paymentMethod = paymentMethod.equals("credit_card") ? "Credit Card" : 
                          paymentMethod.equals("debit_card") ? "Debit Card" : paymentMethod;
            table.addCell(new Phrase(paymentMethod, CONTENT_FONT));
            
            String paymentDate = payment.get("paymentDate").toString();
            table.addCell(new Phrase(paymentDate, CONTENT_FONT));
        }
        
        document.add(table);
    }

    private void addTermsAndConditions(Document document) throws DocumentException {
        Paragraph termsTitle = new Paragraph("TERMS AND CONDITIONS", HEADER_FONT);
        termsTitle.setSpacingBefore(15f);
        document.add(termsTitle);
        
        List<String> terms = List.of(
            "1. Check-in time is 2:00 PM and check-out time is 12:00 PM.",
            "2. Early check-in and late check-out are subject to availability and may incur additional charges.",
            "3. Cancellations made 48 hours prior to arrival will not incur any charges.",
            "4. No-shows and cancellations within 48 hours will be charged for one night's stay.",
            "5. Valid photo identification is required at check-in.",
            "6. The hotel reserves the right to charge for any damages caused to the room during your stay."
        );
        
        for (String term : terms) {
            document.add(new Paragraph(term, SMALL_FONT));
        }
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20f);
        
        Paragraph thankYou = new Paragraph("Thank you for choosing our hotel!", new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC));
        thankYou.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph contact = new Paragraph("For any inquiries, please contact us at Revuzz@hotel.com or + 91 9876543210", SMALL_FONT);
        contact.setAlignment(Element.ALIGN_CENTER);
        
        footer.add(thankYou);
        footer.add(contact);
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
    public byte[] generateStaffPdf(HotelModel staff) throws DocumentException {
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
        addTableRow(table, "Staff ID:", staff.getStaffId().toString());
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
}