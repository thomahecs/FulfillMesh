package com.fulfillmesh.fulfillment.service;

import com.fulfillmesh.fulfillment.dto.FulfillmentView;
import com.fulfillmesh.fulfillment.entity.FulfillmentTask;
import com.fulfillmesh.fulfillment.repository.FulfillmentTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FulfillmentQueryService {
    private final FulfillmentTaskRepository tasks;

    public FulfillmentQueryService(FulfillmentTaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public FulfillmentView byOrderId(UUID orderId) {
        FulfillmentTask task = tasks.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Fulfillment task not found"));
        return new FulfillmentView(
                task.getId(),
                task.getOrderId(),
                task.getState(),
                task.getOptimizedDistance(),
                task.getSequentialDistance(),
                task.getImprovementPercent(),
                task.getPickRoute(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
