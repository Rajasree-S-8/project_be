package com.backend_project.hotel.service;

import com.backend_project.hotel.model.OrderModel;
import com.backend_project.hotel.model.OrderRequest;

import java.util.List;

public interface OrderService {
    OrderModel createOrder(OrderRequest orderRequest, Integer customerId);
    void cancelOrder(Integer orderId, Integer customerId);
    List<OrderModel> getCustomerOrders(Integer customerId);
    OrderModel getOrderDetails(Integer orderId, Integer customerId);
    void confirmPayment(Integer orderId, Integer customerId, Integer paymentId);
}