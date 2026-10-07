package com.fulfillmesh.fulfillment.service;

import com.fulfillmesh.fulfillment.entity.FulfillmentTask;
import com.fulfillmesh.fulfillment.repository.FulfillmentTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FulfillmentQueryServiceTest {
    private final FulfillmentTaskRepository repository = mock(FulfillmentTaskRepository.class);
    private final FulfillmentQueryService service = new FulfillmentQueryService(repository);

    @Test
    void returnsTaskByOrder() {
        UUID orderId = UUID.randomUUID();
        FulfillmentTask task = new FulfillmentTask(UUID.randomUUID(), orderId, Instant.now());
        task.startPicking();
        task.complete(80, 102, 21.5686, "route");
        when(repository.findByOrderId(orderId)).thenReturn(Optional.of(task));

        var view = service.byOrderId(orderId);

        assertEquals(orderId, view.orderId());
        assertEquals(80, view.optimizedDistance());
    }

    @Test
    void throwsWhenTaskMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.byOrderId(orderId));
    }
    @Test
    void listsFulfillmentTasks() {
        UUID orderId = UUID.randomUUID();
        FulfillmentTask task = new FulfillmentTask(UUID.randomUUID(), orderId, Instant.now());
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(task));

        var result = service.all();

        assertEquals(1, result.size());
        assertEquals(orderId, result.getFirst().orderId());
    }

}
