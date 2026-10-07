package com.fulfillmesh.fulfillment.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_tasks")
public class FulfillmentTask {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentState state;

    @Column(name = "optimized_distance")
    private Integer optimizedDistance;

    @Column(name = "sequential_distance")
    private Integer sequentialDistance;

    @Column(name = "improvement_percent")
    private Double improvementPercent;

    @Column(name = "pick_route", columnDefinition = "TEXT")
    private String pickRoute;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FulfillmentTask() {
    }

    public FulfillmentTask(UUID id, UUID orderId, Instant now) {
        this.id = id;
        this.orderId = orderId;
        this.state = FulfillmentState.CREATED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void startPicking() {
        this.state = FulfillmentState.PICKING;
        this.updatedAt = Instant.now();
    }

    public void complete(int optimizedDistance, int sequentialDistance, double improvementPercent, String pickRoute) {
        this.state = FulfillmentState.COMPLETED;
        this.optimizedDistance = optimizedDistance;
        this.sequentialDistance = sequentialDistance;
        this.improvementPercent = improvementPercent;
        this.pickRoute = pickRoute;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public FulfillmentState getState() { return state; }
    public Integer getOptimizedDistance() { return optimizedDistance; }
    public Integer getSequentialDistance() { return sequentialDistance; }
    public Double getImprovementPercent() { return improvementPercent; }
    public String getPickRoute() { return pickRoute; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
