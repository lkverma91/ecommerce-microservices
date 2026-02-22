package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.OrderPlacedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-placed", groupId = "inventory-service")
    public void consumeOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            log.info("Received OrderPlacedEvent: orderId={}", event.getOrderId());
            inventoryService.handleOrderPlaced(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse OrderPlacedEvent: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent: {}", e.getMessage());
        }
    }
}
