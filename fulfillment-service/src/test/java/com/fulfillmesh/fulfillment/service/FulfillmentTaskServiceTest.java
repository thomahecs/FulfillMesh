package com.fulfillmesh.fulfillment.service;

import com.fulfillmesh.common.events.InventoryReservedEvent;
import com.fulfillmesh.common.events.PickStopPayload;
import com.fulfillmesh.common.events.ReservedItemPayload;
import com.fulfillmesh.fulfillment.entity.FulfillmentTask;
import com.fulfillmesh.fulfillment.repository.FulfillmentTaskRepository;
import com.fulfillmesh.fulfillment.routing.GridPoint;
import com.fulfillmesh.fulfillment.routing.PickingRoutePlanner;
import com.fulfillmesh.fulfillment.routing.RoutePlan;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FulfillmentTaskServiceTest {
    private final FulfillmentTaskRepository repository = mock(FulfillmentTaskRepository.class);
    private final PickingRoutePlanner planner = mock(PickingRoutePlanner.class);
    private final FulfillmentTaskService service = new FulfillmentTaskService(repository, planner);

    @Test
    void createsAndCompletesVersionedTask() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(),
                orderId,
                List.of(new ReservedItemPayload("SKU-A", 1, 2, 12)),
                Instant.now()
        );
        RoutePlan route = new RoutePlan(
                List.of(new PickStopPayload("SKU-A", 2, 12)),
                List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0)),
                2,
                3,
                33.333
        );
        when(planner.plan(event.items())).thenReturn(route);
        when(repository.saveAndFlush(any(FulfillmentTask.class))).thenAnswer(i -> i.getArgument(0));

        RoutePlan result = service.createTaskAndRoute(event);

        assertEquals(2, result.optimizedDistance());
        verify(repository, times(2)).saveAndFlush(any(FulfillmentTask.class));
    }
}
