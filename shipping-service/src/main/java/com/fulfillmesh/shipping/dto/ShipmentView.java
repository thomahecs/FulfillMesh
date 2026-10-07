package com.fulfillmesh.shipping.dto;

import com.fulfillmesh.shipping.entity.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentView(
        UUID id,
        UUID orderId,
        String trackingNumber,
        ShipmentStatus status,
        long version,
        Instant createdAt
) {
}
