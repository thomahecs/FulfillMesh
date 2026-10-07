package com.fulfillmesh.shipping.service;

import com.fulfillmesh.shipping.dto.ShipmentView;
import com.fulfillmesh.shipping.entity.Shipment;
import com.fulfillmesh.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShippingQueryService {
    private final ShipmentRepository shipments;

    public ShippingQueryService(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    @Transactional(readOnly = true)
    public ShipmentView byOrderId(UUID orderId) {
        Shipment shipment = shipments.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
        return new ShipmentView(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                shipment.getVersion(),
                shipment.getCreatedAt()
        );
    }
}
