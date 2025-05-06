package com.ecommerce.project.service;

import com.ecommerce.project.payload.OrderDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface OrderService {
    @Transactional
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);
    List<OrderDTO> getOrdersByUserEmail(String email);
    OrderDTO getOrderByIdAndUserEmail(Long orderId, String email);
    List<OrderDTO> getAllOrders();
    OrderDTO getOrderById(Long orderId);
}
