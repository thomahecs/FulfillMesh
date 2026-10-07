package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.ReservedItemPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingBenchmarkTest {

    @Test
    void benchmarkMatchesResumeResult() {
        PickingRoutePlanner planner = new PickingRoutePlanner(
                new AStarRouter(WarehouseGrid.defaultLayout())
        );

        // This order is deterministic and uses the same SKU coordinates as the
        // seeded inventory data. The sequential baseline travels 102 grid cells.
        List<ReservedItemPayload> order = List.of(
                item("SKU-A", 2, 12),
                item("SKU-B", 5, 12),
                item("SKU-C", 5, 2),
                item("SKU-D", 8, 7),
                item("SKU-E", 14, 12),
                item("SKU-F", 11, 12),
                item("SKU-G", 17, 17),
                item("SKU-H", 11, 7)
        );

        RoutePlan plan = planner.plan(order);

        assertEquals(102, plan.sequentialDistance());
        assertEquals(80, plan.optimizedDistance());
        assertEquals(22, Math.round(plan.improvementPercent()));
        assertTrue(plan.improvementPercent() > 21.5);
    }

    @Test
    void emptyOrderStaysAtPackingStation() {
        PickingRoutePlanner planner = new PickingRoutePlanner(
                new AStarRouter(WarehouseGrid.defaultLayout())
        );

        RoutePlan plan = planner.plan(List.of());

        assertEquals(0, plan.optimizedDistance());
        assertEquals(0, plan.sequentialDistance());
        assertEquals(List.of(new GridPoint(0, 0)), plan.optimizedPath());
    }

    private ReservedItemPayload item(String sku, int x, int y) {
        return new ReservedItemPayload(sku, 1, x, y);
    }
}
