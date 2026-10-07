package com.fulfillmesh.fulfillment.service;

import com.fulfillmesh.common.events.InventoryReservedEvent;
import com.fulfillmesh.fulfillment.entity.FulfillmentTask;
import com.fulfillmesh.fulfillment.repository.FulfillmentTaskRepository;
import com.fulfillmesh.fulfillment.routing.GridPoint;
import com.fulfillmesh.fulfillment.routing.PickingRoutePlanner;
import com.fulfillmesh.fulfillment.routing.RoutePlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FulfillmentTaskService {
    private final FulfillmentTaskRepository tasks;
    private final PickingRoutePlanner planner;

    public FulfillmentTaskService(FulfillmentTaskRepository tasks, PickingRoutePlanner planner) {
        this.tasks = tasks;
        this.planner = planner;
    }

    @Transactional
    public RoutePlan createTaskAndRoute(InventoryReservedEvent event) {
        FulfillmentTask task = new FulfillmentTask(UUID.randomUUID(), event.orderId(), Instant.now());
        task.startPicking();
        tasks.saveAndFlush(task);

        RoutePlan route = planner.plan(event.items());

        // The task has @Version, so a stale concurrent update is rejected.
        task.complete(
                route.optimizedDistance(),
                route.sequentialDistance(),
                route.improvementPercent(),
                route.optimizedPath().stream()
                        .map(GridPoint::toString)
                        .collect(Collectors.joining(" -> "))
        );
        tasks.saveAndFlush(task);
        return route;
    }
}
