package com.fulfillmesh.shipping.service;

import com.fulfillmesh.shipping.entity.Shipment;
import com.fulfillmesh.shipping.entity.ShipmentStatus;
import com.fulfillmesh.shipping.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShippingProcessorTest {
    private final ShipmentRepository repository = mock(ShipmentRepository.class);
    private final ShipmentCreationService service = new ShipmentCreationService(repository);

    @Test
    void createsLabelReadyShipment() {
        UUID orderId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        Shipment shipment = service.create(orderId);

        assertEquals(orderId, shipment.getOrderId());
        assertEquals(ShipmentStatus.LABEL_READY, shipment.getStatus());
        assertTrue(shipment.getTrackingNumber().startsWith("FM-"));
    }

    @Test
    void reusesExistingShipment() {
        UUID orderId = UUID.randomUUID();
        Shipment existing = new Shipment(UUID.randomUUID(), orderId, "FM-EXISTING", java.time.Instant.now());
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        assertSame(existing, service.create(orderId));
        verify(repository, never()).saveAndFlush(any());
    }
}
