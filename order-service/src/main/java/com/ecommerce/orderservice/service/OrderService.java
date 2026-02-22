package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.Order.OrderStatus;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.exception.ValidationException;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        userClient.getUserById(request.getUserId());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductResponse product = productClient.getProductById(itemReq.getProductId());
            if (product == null || !Boolean.TRUE.equals(product.getActive())) {
                throw new ValidationException("Product not found or inactive: " + itemReq.getProductId());
            }

            Boolean inStock = inventoryClient.checkStock(itemReq.getProductId(), itemReq.getQuantity());
            if (inStock == null || !inStock) {
                throw new ValidationException("Insufficient stock for product: " + itemReq.getProductId());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            orderItems.add(item);
        }

        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);
        order = orderRepository.save(order);

        publishOrderPlacedEvent(order, orderItems);

        return mapToResponse(order);
    }

    private void publishOrderPlacedEvent(Order order, List<OrderItem> orderItems) {
        try {
            List<OrderPlacedEvent.OrderItemDto> items = orderItems.stream()
                    .map(i -> OrderPlacedEvent.OrderItemDto.builder()
                            .productId(i.getProductId())
                            .quantity(i.getQuantity())
                            .price(i.getUnitPrice().doubleValue())
                            .build())
                    .collect(Collectors.toList());
            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .items(items)
                    .totalAmount(order.getTotalAmount().doubleValue())
                    .build();
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-placed", json);
            log.info("Published OrderPlacedEvent for orderId={}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish OrderPlacedEvent: {}", e.getMessage());
        }
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .collect(Collectors.toList());
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
