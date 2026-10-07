package com.fulfillmesh.shipping.service;

import com.fulfillmesh.shipping.entity.Shipment;
import com.fulfillmesh.shipping.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShippingQueryServiceTest {
    private final ShipmentRepository repository = mock(ShipmentRepository.class);
    private final ShippingQueryService service = new ShippingQueryService(repository);

    @Test
    void returnsShipment() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(UUID.randomUUID(), orderId, "FM-123", Instant.now());
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        assertEquals("FM-123", service.byOrderId(orderId).trackingNumber());
    }

    @Test
    void throwsWhenMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.byOrderId(orderId));
    }
    @Test
    void listsShipments() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(UUID.randomUUID(), orderId, "FM-LIST", Instant.now());
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(shipment));

        var result = service.all();

        assertEquals(1, result.size());
        assertEquals("FM-LIST", result.getFirst().trackingNumber());
    }

}
