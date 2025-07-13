package com.backend_project.hotel.service;

import com.backend_project.hotel.model.*;
import com.backend_project.hotel.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Override
    @Transactional
    public OrderModel createOrder(OrderRequest orderRequest, Integer customerId) {
        CustomerModel customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new RuntimeException("Order must contain at least one item");
        }

        double totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getItems()) {
            if (itemRequest.getQuantity() <= 0) {
                throw new RuntimeException("Quantity must be greater than 0 for food item: " + itemRequest.getFoodId());
            }

            FoodModel food = foodRepository.findById(itemRequest.getFoodId())
                    .orElseThrow(() -> new RuntimeException("Food item not found: " + itemRequest.getFoodId()));

            if (!food.getIsAvailable()) {
                throw new RuntimeException("Food item not available: " + food.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setFood(food);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(food.getPrice() * itemRequest.getQuantity());
            orderItems.add(orderItem);

            totalAmount += orderItem.getPrice();
        }

        OrderModel order = new OrderModel();
        order.setCustomer(customer);
        order.setTotalAmount(totalAmount);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("pending");
        order.setDeliveryAddress(orderRequest.getDeliveryAddress());

        OrderModel savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
            orderItemRepository.save(orderItem);
        }

        // Create a pending payment
        PaymentsModel payment = new PaymentsModel();
        payment.setOrder(savedOrder);
        payment.setAmount(totalAmount);
        payment.setStatus("pending");
        payment.setPaymentMethod("card"); // Default payment method
        payment.setTransactionId("TXN_" + System.currentTimeMillis()); // Mock transaction ID
        payment.setPaymentDate(LocalDateTime.now());
        paymentsRepository.save(payment);

        savedOrder.setItems(orderItems);
        savedOrder.setPayments(List.of(payment));
        return savedOrder;
    }

    @Override
    public void cancelOrder(Integer orderId, Integer customerId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        if (!order.getStatus().equals("pending")) {
            throw new RuntimeException("Cannot cancel an order that is not pending");
        }

        order.setStatus("cancelled");
        orderRepository.save(order);
    }

    @Override
    public List<OrderModel> getCustomerOrders(Integer customerId) {
        return orderRepository.findByCustomerUserId(customerId);
    }

    @Override
    public OrderModel getOrderDetails(Integer orderId, Integer customerId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Unauthorized to view this order");
        }

        return order;
    }

    @Override
    @Transactional
    public void confirmPayment(Integer orderId, Integer customerId, Integer paymentId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Unauthorized to confirm payment for this order");
        }

        if (!order.getStatus().equals("pending")) {
            throw new RuntimeException("Cannot confirm payment for non-pending order");
        }

        PaymentsModel payment = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getOrder().getOrderId().equals(orderId)) {
            throw new RuntimeException("Payment does not belong to this order");
        }

        // Update payment status
        payment.setStatus("completed");
        payment.setPaymentDate(LocalDateTime.now());
        paymentsRepository.save(payment);

        // Update order status
        order.setStatus("completed");
        orderRepository.save(order);
    }
}