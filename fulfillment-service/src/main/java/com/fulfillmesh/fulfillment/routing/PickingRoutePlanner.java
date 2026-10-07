package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.PickStopPayload;
import com.fulfillmesh.common.events.ReservedItemPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PickingRoutePlanner {
    private static final GridPoint PACKING_STATION = new GridPoint(0, 0);
    private final AStarRouter router;

    public PickingRoutePlanner(AStarRouter router) {
        this.router = router;
    }

    public RoutePlan plan(List<ReservedItemPayload> items) {
        if (items == null || items.isEmpty()) {
            return new RoutePlan(List.of(), List.of(PACKING_STATION), 0, 0, 0.0);
        }

        // Baseline: visit items in the incoming order and use A* only to move
        // between consecutive stops.
        int sequentialDistance = distanceForOrder(items);

        // Optimized route: at each stop, choose the remaining item with the
        // shortest actual A* path from the current location. This is deliberately
        // small and readable instead of hiding the routing logic behind a library.
        List<ReservedItemPayload> remaining = new ArrayList<>(items);
        List<ReservedItemPayload> ordered = new ArrayList<>();
        GridPoint current = PACKING_STATION;

        while (!remaining.isEmpty()) {
            GridPoint from = current;
            ReservedItemPayload next = remaining.stream()
                    .min(Comparator
                            .comparingInt((ReservedItemPayload item) ->
                                    router.shortestPath(from, point(item)).distance())
                            .thenComparing(ReservedItemPayload::sku))
                    .orElseThrow();

            ordered.add(next);
            remaining.remove(next);
            current = point(next);
        }

        List<GridPoint> path = buildPath(ordered);
        int optimizedDistance = path.size() - 1;
        double improvement = sequentialDistance == 0
                ? 0.0
                : ((sequentialDistance - optimizedDistance) * 100.0 / sequentialDistance);

        List<PickStopPayload> stops = ordered.stream()
                .map(item -> new PickStopPayload(item.sku(), item.x(), item.y()))
                .toList();

        return new RoutePlan(stops, path, optimizedDistance, sequentialDistance, improvement);
    }

    int distanceForOrder(List<ReservedItemPayload> items) {
        return buildPath(items).size() - 1;
    }

    private List<GridPoint> buildPath(List<ReservedItemPayload> items) {
        List<GridPoint> fullPath = new ArrayList<>();
        fullPath.add(PACKING_STATION);
        GridPoint current = PACKING_STATION;

        for (ReservedItemPayload item : items) {
            current = appendSegment(fullPath, current, point(item));
        }

        appendSegment(fullPath, current, PACKING_STATION);
        return List.copyOf(fullPath);
    }

    private GridPoint appendSegment(List<GridPoint> fullPath, GridPoint from, GridPoint to) {
        PathResult segment = router.shortestPath(from, to);
        // Skip index 0 because it is already the last point in fullPath.
        for (int i = 1; i < segment.path().size(); i++) {
            fullPath.add(segment.path().get(i));
        }
        return to;
    }

    private GridPoint point(ReservedItemPayload item) {
        return new GridPoint(item.x(), item.y());
    }
}
