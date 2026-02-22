package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.InventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.OrderPlacedEvent;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.ResourceNotFoundException;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryResponse createOrUpdate(InventoryRequest request) {
        Inventory inv = inventoryRepository.findByProductId(request.getProductId())
                .orElse(Inventory.builder().productId(request.getProductId()).quantity(0).reserved(0).build());
        inv.setQuantity(request.getQuantity());
        inv = inventoryRepository.save(inv);
        return mapToResponse(inv);
    }

    public InventoryResponse getByProductId(Long productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return mapToResponse(inv);
    }

    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean checkStock(Long productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(inv -> (inv.getQuantity() - inv.getReserved()) >= quantity)
                .orElse(false);
    }

    @Transactional
    public void reserveStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        int available = inv.getQuantity() - inv.getReserved();
        if (available < quantity) {
            throw new InsufficientStockException("Insufficient stock for product " + productId);
        }
        inv.setReserved(inv.getReserved() + quantity);
        inventoryRepository.save(inv);
    }

    @Transactional
    public void releaseStock(Long productId, int quantity) {
        inventoryRepository.findByProductId(productId).ifPresent(inv -> {
            inv.setReserved(Math.max(0, inv.getReserved() - quantity));
            inventoryRepository.save(inv);
        });
    }

    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent event) {
        for (OrderPlacedEvent.OrderItemDto item : event.getItems()) {
            reserveStock(item.getProductId(), item.getQuantity());
        }
    }

    private InventoryResponse mapToResponse(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId())
                .productId(inv.getProductId())
                .quantity(inv.getQuantity())
                .reserved(inv.getReserved())
                .available(inv.getQuantity() - inv.getReserved())
                .build();
    }
}
