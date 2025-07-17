package com.backend_project.hotel.service;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Override
    @Transactional
    public ResponseEntity<?> createOrder(JsonNode orderRequest, Integer customerId) {
        try {
            Optional<CustomerModel> customer = customerRepository.findById(customerId);
            if (customer.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found with ID: " + customerId);
            }

            String deliveryAddress = orderRequest.has("deliveryAddress") 
                ? orderRequest.get("deliveryAddress").asText() 
                : null;
            if (deliveryAddress == null || deliveryAddress.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Delivery address is required");
            }

            OrderModel order = new OrderModel();
            order.setCustomer(customer.get());
            order.setOrderDate(LocalDateTime.now());
            order.setDeliveryAddress(deliveryAddress);
            order.setStatus("pending");

            OrderModel savedOrder = orderRepository.save(order);
            System.out.println("Saved order: ID=" + savedOrder.getOrderId() + ", status=" + savedOrder.getStatus());

            List<OrderItemModel> orderItems = new ArrayList<>();
            double totalAmount = 0;

            JsonNode itemsNode = orderRequest.get("items");
            if (itemsNode == null || !itemsNode.isArray()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Items array is required");
            }

            for (JsonNode itemNode : itemsNode) {
                Integer foodId = itemNode.has("foodId") ? itemNode.get("foodId").asInt() : null;
                Integer quantity = itemNode.has("quantity") ? itemNode.get("quantity").asInt() : null;

                if (foodId == null || quantity == null || quantity <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid foodId or quantity in items");
                }

                Optional<FoodModel> food = foodRepository.findById(foodId);
                if (food.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Food item with ID " + foodId + " not found");
                }
                if (!food.get().getIsAvailable()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Food item " + food.get().getName() + " is not available");
                }

                OrderItemModel orderItem = new OrderItemModel();
                orderItem.setOrder(savedOrder);
                orderItem.setFood(food.get());
                orderItem.setQuantity(quantity);
                orderItem.setPriceAtOrder(food.get().getPrice());
                orderItems.add(orderItem);
                totalAmount += food.get().getPrice() * quantity;
            }

            orderItemRepository.saveAll(orderItems);
            savedOrder.setItems(orderItems);

            PaymentsModel payment = new PaymentsModel();
            payment.setOrder(savedOrder);
            payment.setAmount(totalAmount);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentMethod("credit_card");
            payment.setStatus("pending");
            payment.setTransactionId(UUID.randomUUID().toString());

            PaymentsModel savedPayment = paymentsRepository.save(payment);
            System.out.println("Saved payment: ID=" + savedPayment.getPaymentId() + ", status=" + savedPayment.getStatus());
            savedOrder.setPayments(List.of(savedPayment));

            return ResponseEntity.status(HttpStatus.CREATED).body(new OrderResponseDTO(savedOrder));
        } catch (Exception e) {
            System.err.println("Error creating order for customer ID " + customerId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> processPayment(Integer orderId, JsonNode paymentRequest, Integer customerId) {
        try {
            Optional<OrderModel> order = orderRepository.findById(orderId);
            if (order.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + orderId + " not found");
            }
            if (!order.get().getCustomer().getUserId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Order does not belong to customer with ID " + customerId);
            }
            
            List<PaymentsModel> payments = paymentsRepository.findByOrderOrderId(orderId);
            if (payments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No payment found for order ID " + orderId);
            }

            String cardLastFour = paymentRequest.has("cardLastFour") 
                ? paymentRequest.get("cardLastFour").asText() 
                : null;
            if (cardLastFour == null || !cardLastFour.matches("\\d{4}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Card last four digits must be exactly 4 digits");
            }

            System.out.println("Processing payment for order ID: " + orderId + ", customer ID: " + customerId + ", cardLastFour: " + cardLastFour);

            PaymentsModel paymentToUpdate = payments.get(0);
            paymentToUpdate.setStatus("completed");
            paymentToUpdate.setCardLastFour(cardLastFour);
            paymentToUpdate.setPaymentDate(LocalDateTime.now());
            
            PaymentsModel updatedPayment = paymentsRepository.save(paymentToUpdate);
            System.out.println("Updated payment: ID=" + updatedPayment.getPaymentId() + ", status=" + updatedPayment.getStatus());
            
            OrderModel orderToUpdate = order.get();
            orderToUpdate.setStatus("completed");
            OrderModel updatedOrder = orderRepository.save(orderToUpdate);
            System.out.println("Updated order: ID=" + updatedOrder.getOrderId() + ", status=" + updatedOrder.getStatus());
            
            return ResponseEntity.ok(new OrderResponseDTO(updatedOrder, "Payment processed successfully for order ID " + orderId));
        } catch (Exception e) {
            System.err.println("Error processing payment for order ID " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process payment for order ID " + orderId + ": " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOrderDetails(Integer orderId, Integer customerId) {
        Optional<OrderModel> order = orderRepository.findById(orderId);
        if (order.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!order.get().getCustomer().getUserId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new OrderResponseDTO(order.get()));
    }

    @Override
    public ResponseEntity<?> getCustomerOrders(Integer customerId) {
        List<OrderModel> orders = orderRepository.findByCustomerUserId(customerId);
        List<OrderResponseDTO> orderResponses = orders.stream()
                .map(order -> new OrderResponseDTO(order, "Order retrieved successfully"))
                .toList();
        return ResponseEntity.ok(orderResponses);
    }

    // DTO class to avoid circular references
    public static class OrderResponseDTO {
        private final Integer orderId;
        private final Integer customerId;
        private final LocalDateTime orderDate;
        private final String deliveryAddress;
        private final String status;
        private final List<OrderItemDTO> items;
        private final List<PaymentDTO> payments;
        private final String message;

        public OrderResponseDTO(OrderModel order) {
            this(order, null);
        }

        public OrderResponseDTO(OrderModel order, String message) {
            this.orderId = order.getOrderId();
            this.customerId = order.getCustomer().getUserId();
            this.orderDate = order.getOrderDate();
            this.deliveryAddress = order.getDeliveryAddress();
            this.status = order.getStatus();
            this.items = order.getItems().stream()
                    .map(item -> new OrderItemDTO(item))
                    .toList();
            this.payments = order.getPayments().stream()
                    .map(payment -> new PaymentDTO(payment))
                    .toList();
            this.message = message;
        }

        // Getters
        public Integer getOrderId() { return orderId; }
        public Integer getCustomerId() { return customerId; }
        public LocalDateTime getOrderDate() { return orderDate; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public String getStatus() { return status; }
        public List<OrderItemDTO> getItems() { return items; }
        public List<PaymentDTO> getPayments() { return payments; }
        public String getMessage() { return message; }
    }

    public static class OrderItemDTO {
        private final Integer orderItemId;
        private final Integer foodId;
        private final Integer quantity;
        private final Double priceAtOrder;

        public OrderItemDTO(OrderItemModel item) {
            this.orderItemId = item.getOrderItemId();
            this.foodId = item.getFood().getFoodId();
            this.quantity = item.getQuantity();
            this.priceAtOrder = item.getPriceAtOrder();
        }

        // Getters
        public Integer getOrderItemId() { return orderItemId; }
        public Integer getFoodId() { return foodId; }
        public Integer getQuantity() { return quantity; }
        public Double getPriceAtOrder() { return priceAtOrder; }
    }

    public static class PaymentDTO {
        private final Integer paymentId;
        private final Double amount;
        private final LocalDateTime paymentDate;
        private final String paymentMethod;
        private final String status;
        private final String transactionId;
        private final String cardLastFour;

        public PaymentDTO(PaymentsModel payment) {
            this.paymentId = payment.getPaymentId();
            this.amount = payment.getAmount();
            this.paymentDate = payment.getPaymentDate();
            this.paymentMethod = payment.getPaymentMethod();
            this.status = payment.getStatus();
            this.transactionId = payment.getTransactionId();
            this.cardLastFour = payment.getCardLastFour();
        }

        // Getters
        public Integer getPaymentId() { return paymentId; }
        public Double getAmount() { return amount; }
        public LocalDateTime getPaymentDate() { return paymentDate; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
        public String getTransactionId() { return transactionId; }
        public String getCardLastFour() { return cardLastFour; }
    }
    @Override
    public ResponseEntity<byte[]> generateInvoice(Integer orderId, Integer customerId) {
        try {
            Optional<OrderModel> order = orderRepository.findById(orderId);
            if (order.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!order.get().getCustomer().getUserId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Create PDF document
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            // Add invoice content
            document.add(new Paragraph("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph(" "));
            
            // Order details
            document.add(new Paragraph("Order #" + order.get().getOrderId()));
            document.add(new Paragraph("Date: " + order.get().getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
            document.add(new Paragraph("Status: " + order.get().getStatus()));
            document.add(new Paragraph(" "));
            
            // Customer details
            CustomerModel customer = order.get().getCustomer();
            document.add(new Paragraph("Customer: " + customer.getFullName()));
            document.add(new Paragraph("Delivery Address: " + order.get().getDeliveryAddress()));
            document.add(new Paragraph(" "));
            
            // Items table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            // Table headers
            table.addCell(new Phrase("Item", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            table.addCell(new Phrase("Price", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            table.addCell(new Phrase("Qty", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            table.addCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            
            // Table rows
            for (OrderItemModel item : order.get().getItems()) {
                table.addCell(item.getFood().getName());
                table.addCell("₹" + item.getPriceAtOrder());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell("₹" + (item.getPriceAtOrder() * item.getQuantity()));
            }
            
            document.add(table);
            
            // Total
            double subtotal = order.get().getItems().stream()
                    .mapToDouble(item -> item.getPriceAtOrder() * item.getQuantity())
                    .sum();
            
            document.add(new Paragraph("Subtotal: ₹" + subtotal));
            document.add(new Paragraph("Delivery Fee: ₹0.00"));
            document.add(new Paragraph("Tax: ₹0.00"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total: ₹" + subtotal));
            
            document.close();
            
            // Return PDF as response
            byte[] pdfBytes = outputStream.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice_" + orderId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @Override
    @Transactional
    public ResponseEntity<?> cancelOrder(Integer orderId, Integer customerId) {
        try {
            Optional<OrderModel> order = orderRepository.findById(orderId);
            if (order.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + orderId + " not found");
            }
            if (!order.get().getCustomer().getUserId().equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Order does not belong to customer with ID " + customerId);
            }

            // Check if order is cancellable (only pending, processing, or completed orders can be canceled)
            String currentStatus = order.get().getStatus().toLowerCase();
            if (!currentStatus.equals("pending") && !currentStatus.equals("processing") && !currentStatus.equals("completed")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Order with ID " + orderId + " cannot be canceled as it is in " + currentStatus + " status");
            }

            // Update order status to cancelled
            OrderModel orderToUpdate = order.get();
            orderToUpdate.setStatus("cancelled");
            OrderModel updatedOrder = orderRepository.save(orderToUpdate);
            System.out.println("Updated order: ID=" + updatedOrder.getOrderId() + ", status=" + updatedOrder.getStatus());

            // If payment was completed, initiate a refund
            List<PaymentsModel> payments = paymentsRepository.findByOrderOrderId(orderId);
            if (!payments.isEmpty() && "completed".equalsIgnoreCase(payments.get(0).getStatus())) {
                PaymentsModel payment = payments.get(0);
                payment.setStatus("refunded");
                payment.setPaymentDate(LocalDateTime.now());
                paymentsRepository.save(payment);
                System.out.println("Refunded payment: ID=" + payment.getPaymentId() + ", status=" + payment.getStatus());
            }

            return ResponseEntity.ok(new OrderResponseDTO(updatedOrder, "Order cancelled successfully"));
        } catch (Exception e) {
            System.err.println("Error cancelling order ID " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to cancel order: " + e.getMessage());
        }
    }
}
