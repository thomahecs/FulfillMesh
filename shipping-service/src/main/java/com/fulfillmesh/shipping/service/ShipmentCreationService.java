package com.fulfillmesh.shipping.service;

import com.fulfillmesh.shipping.entity.Shipment;
import com.fulfillmesh.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class ShipmentCreationService {
    private final ShipmentRepository shipments;

    public ShipmentCreationService(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    @Transactional
    public Shipment create(UUID orderId) {
        return shipments.findByOrderId(orderId).orElseGet(() -> {
            UUID shipmentId = UUID.randomUUID();

            // A real carrier API would return this value. Deriving it from the
            // shipment id keeps local demos reproducible without external accounts.
            String tracking = "FM-" + shipmentId.toString()
                    .replace("-", "")
                    .substring(0, 14)
                    .toUpperCase(Locale.ROOT);

            Shipment shipment = new Shipment(shipmentId, orderId, tracking, Instant.now());
            shipment.markLabelReady();
            return shipments.saveAndFlush(shipment);
        });
    }
}
