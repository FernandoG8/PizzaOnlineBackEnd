package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.payload.PaymentStatusUpdateDTO;
import com.ecommerce.project.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.CannotAcquireLockException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;


@Service
@Transactional
@Retryable(
        value = {CannotAcquireLockException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000) // 1 segundo entre reintentos
)
public class OrderServiceImpl implements OrderService {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CartService cartService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ProductRepository productRepository;
    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setAddress(address);

        Payment payment;
        if ("CASH".equals(paymentMethod.toUpperCase())) {
            // Configuración específica para pago en efectivo
            order.setOrderStatus("Pending Cash Payment");
            payment = new Payment(
                    paymentMethod,
                    null,
                    PaymentStatus.PENDING.getStatus(),
                    "Pending cash payment",
                    "CASH"
            );
        } else {
            // Flujo normal para otros métodos de pago
            order.setOrderStatus("Order Accepted !");
            payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        }

        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()) {
            throw new APIException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        orderItems = orderItemRepository.saveAll(orderItems);

        cart.getCartItems().forEach(item -> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() - quantity);
            productRepository.save(product);
            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
        });

        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(item -> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));
        orderDTO.setAddressId(addressId);

        return orderDTO;
    }

    @Override
    public List<OrderDTO> getOrdersByUserEmail(String email) {
        List<Order> orders = orderRepository.findByEmail(email);
        return orders.stream().map(this::mapToOrderDTO).toList();
    }

    @Override
    public OrderDTO getOrderByIdAndUserEmail(Long orderId, String email) {
        Order order = orderRepository.findByOrderIdAndEmail(orderId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return mapToOrderDTO(order);
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::mapToOrderDTO).toList();
    }

    @Override
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return mapToOrderDTO(order);
    }

    // Método auxiliar para mapear Order a OrderDTO
    private OrderDTO mapToOrderDTO(Order order) {
        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
        List<OrderItemDTO> orderItems = order.getOrderItems().stream()
                .map(item -> modelMapper.map(item, OrderItemDTO.class))
                .toList();
        orderDTO.setOrderItems(orderItems);
        return orderDTO;
    }
    @Override
    @Transactional
    public OrderDTO updatePaymentStatus(Long orderId, PaymentStatusUpdateDTO dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        Payment payment = order.getPayment();


        System.out.println("Estado recibido: " + dto.getPgStatus());
        PaymentStatus status = PaymentStatus.fromString(dto.getPgStatus());
        System.out.println("Estado convertido: " + status);

        payment.setPgStatus(dto.getPgStatus());
        payment.setPgResponseMessage(dto.getPgResponseMessage());

        switch (status) {
            case COMPLETED -> {
                order.setOrderStatus("Completada");
                System.out.println("Cambiando estado a Completada");
            }
            case CANCELLED -> {
                order.setOrderStatus("Cancelada");
                System.out.println("Cambiando estado a Cancelada");
            }
            case PENDING -> {
                order.setOrderStatus("La Orden está Pendiente !");
                System.out.println("Cambiando estado a Pendiente");
            }
        }

        paymentRepository.save(payment);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderDTO(updatedOrder);
    }

}