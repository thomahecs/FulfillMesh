package com.fulfillmesh.shipping.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Shipment() {
    }

    public Shipment(UUID id, UUID orderId, String trackingNumber, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
        this.status = ShipmentStatus.CREATED;
        this.createdAt = createdAt;
    }

    public void markLabelReady() {
        this.status = ShipmentStatus.LABEL_READY;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
