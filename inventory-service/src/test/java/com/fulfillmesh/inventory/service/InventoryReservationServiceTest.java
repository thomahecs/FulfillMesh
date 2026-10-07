package com.fulfillmesh.inventory.service;

import com.fulfillmesh.common.events.OrderCreatedEvent;
import com.fulfillmesh.common.events.OrderLinePayload;
import com.fulfillmesh.inventory.entity.InventoryItem;
import com.fulfillmesh.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryReservationServiceTest {
    private final InventoryRepository inventory = mock(InventoryRepository.class);
    private final InventoryReservationService service = new InventoryReservationService(inventory);

    @Test
    void reservesStockAcrossMultipleRows() {
        InventoryItem a = new InventoryItem("SKU-A", "A", 10, 2, 12);
        InventoryItem b = new InventoryItem("SKU-B", "B", 5, 5, 12);
        when(inventory.findBySkuForUpdate("SKU-A")).thenReturn(Optional.of(a));
        when(inventory.findBySkuForUpdate("SKU-B")).thenReturn(Optional.of(b));

        ReservationResult result = service.reserve(event(List.of(
                new OrderLinePayload("SKU-B", 2),
                new OrderLinePayload("SKU-A", 3)
        )));

        assertTrue(result.success());
        assertEquals(7, a.getAvailableQuantity());
        assertEquals(3, a.getReservedQuantity());
        assertEquals(3, b.getAvailableQuantity());
        assertEquals(2, result.reservedItems().size());
        verify(inventory).flush();
    }

    @Test
    void rejectsUnknownSkuWithoutChangingOtherRows() {
        InventoryItem a = new InventoryItem("SKU-A", "A", 10, 2, 12);
        when(inventory.findBySkuForUpdate("SKU-A")).thenReturn(Optional.of(a));
        when(inventory.findBySkuForUpdate("SKU-Z")).thenReturn(Optional.empty());

        ReservationResult result = service.reserve(event(List.of(
                new OrderLinePayload("SKU-A", 3),
                new OrderLinePayload("SKU-Z", 1)
        )));

        assertFalse(result.success());
        assertEquals(10, a.getAvailableQuantity());
        verify(inventory, never()).saveAll(any());
    }

    @Test
    void rejectsInsufficientStockWithoutDecrement() {
        InventoryItem a = new InventoryItem("SKU-A", "A", 1, 2, 12);
        when(inventory.findBySkuForUpdate("SKU-A")).thenReturn(Optional.of(a));

        ReservationResult result = service.reserve(event(List.of(new OrderLinePayload("SKU-A", 2))));

        assertFalse(result.success());
        assertTrue(result.reason().contains("Insufficient"));
        assertEquals(1, a.getAvailableQuantity());
    }

    private OrderCreatedEvent event(List<OrderLinePayload> lines) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "student@example.com",
                lines,
                Instant.now()
        );
    }
}
