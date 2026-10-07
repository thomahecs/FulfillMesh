package com.fulfillmesh.order.service;

import com.fulfillmesh.order.entity.OrderEntity;
import com.fulfillmesh.order.entity.OrderState;
import com.fulfillmesh.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderStateServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderStateService service = new OrderStateService(repository);

    @Test
    void advancesStateAndFlushesVersionedEntity() {
        UUID id = UUID.randomUUID();
        OrderEntity order = new OrderEntity(id, "student@example.com", Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(order));

        service.move(id, OrderState.INVENTORY_RESERVED);

        assertEquals(OrderState.INVENTORY_RESERVED, order.getState());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void ignoresLateEventThatWouldMoveStateBackward() {
        UUID id = UUID.randomUUID();
        OrderEntity order = new OrderEntity(id, "student@example.com", Instant.now());
        order.moveTo(OrderState.SHIPPED);
        when(repository.findById(id)).thenReturn(Optional.of(order));

        service.move(id, OrderState.PICKING);

        assertEquals(OrderState.SHIPPED, order.getState());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void inventoryRejectionIsTerminal() {
        assertTrue(service.shouldAdvance(OrderState.CREATED, OrderState.INVENTORY_REJECTED));
        assertFalse(service.shouldAdvance(OrderState.INVENTORY_REJECTED, OrderState.SHIPPED));
    }
}
