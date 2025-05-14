package com.ecommerce.project.controller;

import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderRequestDTO;
import com.ecommerce.project.payload.PaymentStatusUpdateDTO;
import com.ecommerce.project.payload.StripePaymentDto;
import com.ecommerce.project.service.OrderService;
import com.ecommerce.project.service.StripeService;
import com.ecommerce.project.util.AuthUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private StripeService stripeService;

    @Autowired
    private StripeService stripeService;

    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(
            @PathVariable String paymentMethod,
            @RequestBody OrderRequestDTO orderRequestDTO) {

        String emailId = authUtil.loggedInEmail();

        if ("CASH".equals(paymentMethod.toUpperCase())) {
            orderRequestDTO.setPgName("CASH");
            orderRequestDTO.setPgPaymentId(null);
            orderRequestDTO.setPgStatus("pending");
            orderRequestDTO.setPgResponseMessage("Pending cash payment");
        }

        OrderDTO order = orderService.placeOrder(
                emailId,
                orderRequestDTO.getAddressId(),
                paymentMethod,
                orderRequestDTO.getPgName(),
                orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(),
                orderRequestDTO.getPgResponseMessage()
        );

        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
    @GetMapping("/user/orders")
    public ResponseEntity<List<OrderDTO>> getUserOrders() {
        String email = authUtil.loggedInEmail();
        List<OrderDTO> orders = orderService.getOrdersByUserEmail(email);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/order/{orderId}")
    public ResponseEntity<OrderDTO> getUserOrderById(@PathVariable Long orderId) {
        String email = authUtil.loggedInEmail();
        OrderDTO order = orderService.getOrderByIdAndUserEmail(orderId, email);
        return ResponseEntity.ok(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/order/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/order/{orderId}/payment-status")
    public ResponseEntity<OrderDTO> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestBody PaymentStatusUpdateDTO paymentStatusUpdateDTO) {
        OrderDTO updatedOrder = orderService.updatePaymentStatus(orderId, paymentStatusUpdateDTO);
        return ResponseEntity.ok(updatedOrder);
    }


    @PostMapping("/order/stripe-client-secret")
    public ResponseEntity<Map<String, String>> createStripeClientSecret(
            @RequestBody StripePaymentDto stripePaymentDto) throws StripeException {
        PaymentIntent paymentIntent = stripeService.paymentIntent(stripePaymentDto);

        Map<String, String> response = new HashMap<>();
        response.put("client_secret", paymentIntent.getClientSecret());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/order/{orderId}/payment-status")
    public ResponseEntity<OrderDTO> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestBody PaymentStatusUpdateDTO paymentStatusUpdateDTO) {
        OrderDTO updatedOrder = orderService.updatePaymentStatus(orderId, paymentStatusUpdateDTO);
        return ResponseEntity.ok(updatedOrder);
    }

}


