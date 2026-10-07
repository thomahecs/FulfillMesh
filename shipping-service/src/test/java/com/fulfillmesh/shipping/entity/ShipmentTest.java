package com.fulfillmesh.shipping.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipmentTest {

    @Test
    void labelCanBecomeReady() {
        Shipment shipment = new Shipment(UUID.randomUUID(), UUID.randomUUID(), "FM-TEST", Instant.now());
        shipment.markLabelReady();

        assertEquals(ShipmentStatus.LABEL_READY, shipment.getStatus());
    }
}
