package com.backend_project.hotel.model;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String deliveryAddress;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Integer foodId;
        private Integer quantity;
    }
}