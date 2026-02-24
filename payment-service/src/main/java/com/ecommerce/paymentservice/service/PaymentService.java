package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.OrderPlacedEvent;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.Payment.PaymentStatus;
import com.ecommerce.paymentservice.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional    //explain @Transactional is a Spring annotation that indicates that the annotated method
    // should be executed within a transactional context. This means that all operations performed within the method
    // will be part of a single transaction. If any operation fails (e.g., an exception is thrown), the entire transaction
    // will be rolled back, ensuring data integrity. In the context of the processOrderPlaced method,
    // it ensures that if saving the payment fails for any reason, any changes made to the database during that method execution
    // will be undone, preventing partial updates and maintaining consistency in the payment records.

    public void processOrderPlaced(OrderPlacedEvent event) {
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(BigDecimal.valueOf(event.getTotalAmount()))
                .status(PaymentStatus.COMPLETED)
                .transactionId("TXN-" + UUID.randomUUID())
                .build();
        paymentRepository.save(payment);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
