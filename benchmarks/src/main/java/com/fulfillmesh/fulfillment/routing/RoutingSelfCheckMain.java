package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.ReservedItemPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure Java regression checks for route optimality and walkable path integrity. */
public final class RoutingSelfCheckMain {
    private RoutingSelfCheckMain() {}

    public static void main(String[] args) {
        WarehouseGrid grid = WarehouseGrid.defaultLayout();
        PickingRoutePlanner planner = new PickingRoutePlanner(new AStarRouter(grid));
        List<GridPoint> candidates = new ArrayList<>();
        for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) {
            GridPoint cell = new GridPoint(x, y);
            if (grid.canWalk(cell) && !cell.equals(new GridPoint(0, 0))) candidates.add(cell);
        }
        Random r = new Random(51);
        for (int t = 0; t < 35; t++) {
            Collections.shuffle(candidates, r);
            List<ReservedItemPayload> items = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                GridPoint p = candidates.get(i);
                items.add(new ReservedItemPayload("SKU-" + i, 1, p.x(), p.y()));
            }
            int exact = planner.plan(items).optimizedDistance();
            int brute = bruteForce(planner, items, 0);
            if (exact != brute) throw new AssertionError("Exact tour differs from brute force, case " + t);
        }
        for (int t = 0; t < 50; t++) {
            Collections.shuffle(candidates, r);
            List<ReservedItemPayload> items = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                GridPoint p = candidates.get(i);
                items.add(new ReservedItemPayload("SKU-" + i, 1, p.x(), p.y()));
            }
            RoutePlan plan = planner.plan(items);
            if (plan.optimizedDistance() > plan.sequentialDistance()
                    || plan.optimizedDistance() > planner.nearestNeighborDistance(items)) {
                throw new AssertionError("Optimizer worse than baseline, case " + t);
            }
            List<GridPoint> path = plan.optimizedPath();
            if (!path.getFirst().equals(new GridPoint(0, 0))
                    || !path.getLast().equals(new GridPoint(0, 0))
                    || path.size() - 1 != plan.optimizedDistance()) {
                throw new AssertionError("Invalid closed walk, case " + t);
            }
            for (int i = 1; i < path.size(); i++) {
                if (!grid.canWalk(path.get(i)) || path.get(i - 1).manhattan(path.get(i)) != 1) {
                    throw new AssertionError("Non-adjacent or blocked walk segment, case " + t);
                }
            }
        }
        System.out.println("PASS: 35 exact-vs-brute-force tours and 50 long-tour path/baseline checks");
    }

    private static int bruteForce(PickingRoutePlanner planner, List<ReservedItemPayload> items, int pos) {
        if (pos == items.size()) return planner.distanceForOrder(items);
        int best = Integer.MAX_VALUE;
        for (int i = pos; i < items.size(); i++) {
            Collections.swap(items, i, pos);
            best = Math.min(best, bruteForce(planner, items, pos + 1));
            Collections.swap(items, i, pos);
        }
        return best;
    }
}
