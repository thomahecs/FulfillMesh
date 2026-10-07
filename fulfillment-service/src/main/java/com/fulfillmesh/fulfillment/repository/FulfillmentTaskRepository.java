package com.fulfillmesh.fulfillment.repository;

import com.fulfillmesh.fulfillment.entity.FulfillmentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FulfillmentTaskRepository extends JpaRepository<FulfillmentTask, UUID> {
    Optional<FulfillmentTask> findByOrderId(UUID orderId);
}
