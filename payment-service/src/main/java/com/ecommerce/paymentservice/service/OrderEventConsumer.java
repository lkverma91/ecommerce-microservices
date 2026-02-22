package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.OrderPlacedEvent;
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

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-placed", groupId = "payment-service")
    public void consumeOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            log.info("Received OrderPlacedEvent: orderId={}", event.getOrderId());
            paymentService.processOrderPlaced(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse OrderPlacedEvent: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent: {}", e.getMessage());
        }
    }
}
