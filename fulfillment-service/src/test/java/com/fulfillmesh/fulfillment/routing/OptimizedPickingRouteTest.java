package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.ReservedItemPayload;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OptimizedPickingRouteTest {
    private final WarehouseGrid warehouse = WarehouseGrid.defaultLayout();
    private final PickingRoutePlanner planner = new PickingRoutePlanner(new AStarRouter(warehouse));

    @Test
    void smallTourMatchesBruteForceOptimum() {
        List<ReservedItemPayload> picks = List.of(
                pick(0, 2, 12), pick(1, 5, 2), pick(2, 8, 7),
                pick(3, 14, 12), pick(4, 17, 17));
        int optimal = bruteForce(new ArrayList<>(picks), 0);
        RoutePlan route = planner.plan(picks);
        assertEquals(optimal, route.optimizedDistance());
        assertEquals(optimal + 1, route.optimizedPath().size());
        assertEquals(new GridPoint(0, 0), route.optimizedPath().getFirst());
        assertEquals(new GridPoint(0, 0), route.optimizedPath().getLast());
    }

    @Test
    void randomizedOrdersNeverRegressAgainstEitherBaseline() {
        Random random = new Random(4321L);
        List<GridPoint> walkable = new ArrayList<>();
        for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) {
            GridPoint point = new GridPoint(x, y);
            if (warehouse.canWalk(point) && !point.equals(new GridPoint(0, 0))) walkable.add(point);
        }

        for (int i = 0; i < 150; i++) {
            Collections.shuffle(walkable, random);
            int stops = 4 + random.nextInt(11); // both exact and 2-opt routes
            List<ReservedItemPayload> picks = new ArrayList<>();
            for (int j = 0; j < stops; j++) {
                GridPoint p = walkable.get(j);
                picks.add(pick(j, p.x(), p.y()));
            }
            RoutePlan plan = planner.plan(picks);
            assertEquals(stops, plan.optimizedStops().size());
            assertEquals(plan.optimizedDistance() + 1, plan.optimizedPath().size());
            assertTrue(plan.optimizedDistance() <= plan.sequentialDistance());
            assertTrue(plan.optimizedDistance() <= planner.nearestNeighborDistance(picks));
            assertEquals(new GridPoint(0, 0), plan.optimizedPath().getFirst());
            assertEquals(new GridPoint(0, 0), plan.optimizedPath().getLast());
            for (int k = 1; k < plan.optimizedPath().size(); k++) {
                assertEquals(1, plan.optimizedPath().get(k - 1)
                        .manhattan(plan.optimizedPath().get(k)));
            }
        }
    }

    @Test
    void nullAndEmptyOrdersStayAtPackingStation() {
        assertEquals(0, planner.plan(null).optimizedDistance());
        assertEquals(0, planner.plan(List.of()).optimizedDistance());
        assertEquals(0, planner.nearestNeighborDistance(List.of()));
    }

    private int bruteForce(List<ReservedItemPayload> candidates, int pos) {
        if (pos == candidates.size()) return planner.distanceForOrder(candidates);
        int best = Integer.MAX_VALUE;
        for (int i = pos; i < candidates.size(); i++) {
            Collections.swap(candidates, pos, i);
            best = Math.min(best, bruteForce(candidates, pos + 1));
            Collections.swap(candidates, pos, i);
        }
        return best;
    }

    private static ReservedItemPayload pick(int index, int x, int y) {
        return new ReservedItemPayload("PICK-" + index, 1, x, y);
    }
}
