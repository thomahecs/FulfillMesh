package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.PickStopPayload;
import com.fulfillmesh.common.events.ReservedItemPayload;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

/** Warehouse-constrained A* leg distances with exact small-tour planning and 2-opt fallback. */
public class PickingRoutePlanner {
    private static final GridPoint PACKING_STATION = new GridPoint(0, 0);
    private final AStarRouter router;
    private final Map<Leg, Integer> distances = new ConcurrentHashMap<>();

    public PickingRoutePlanner(AStarRouter router) {
        this.router = router;
    }

    public RoutePlan plan(List<ReservedItemPayload> items) {
        if (items == null || items.isEmpty()) {
            return new RoutePlan(List.of(), List.of(PACKING_STATION), 0, 0, 0.0);
        }
        int sequential = distanceForOrder(items);
        List<ReservedItemPayload> picked = optimizedOrder(items);
        List<GridPoint> path = buildPath(picked);
        int optimized = path.size() - 1;
        double improvement = sequential == 0 ? 0 : 100.0 * (sequential - optimized) / sequential;
        List<PickStopPayload> stops = picked.stream()
                .map(item -> new PickStopPayload(item.sku(), item.x(), item.y()))
                .toList();
        return new RoutePlan(stops, path, optimized, sequential, improvement);
    }

    public int nearestNeighborDistance(List<ReservedItemPayload> items) {
        return distanceForOrder(nearestNeighbor(items));
    }

    public List<ReservedItemPayload> optimizedOrder(List<ReservedItemPayload> items) {
        if (items.size() <= 10) return exactOrder(items);
        List<ReservedItemPayload> route = improveTwoOpt(nearestNeighbor(items));
        List<ReservedItemPayload> aisleRoute = improveTwoOpt(new ArrayList<>(items));
        return distanceForOrder(aisleRoute) < distanceForOrder(route)
                ? List.copyOf(aisleRoute) : List.copyOf(route);
    }

    /** Held-Karp minimum closed tour over shortest walkable A* legs (up to 10 picks). */
    private List<ReservedItemPayload> exactOrder(List<ReservedItemPayload> items) {
        int n = items.size();
        if (n == 0) return List.of();
        int masks = 1 << n;
        int[][] dp = new int[masks][n];
        int[][] parent = new int[masks][n];
        int infinity = Integer.MAX_VALUE / 4;
        for (int mask = 0; mask < masks; mask++) {
            java.util.Arrays.fill(dp[mask], infinity);
            java.util.Arrays.fill(parent[mask], -1);
        }
        for (int end = 0; end < n; end++) {
            dp[1 << end][end] = distance(PACKING_STATION, point(items.get(end)));
        }
        for (int mask = 1; mask < masks; mask++) {
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0 || dp[mask][last] == infinity) continue;
                for (int next = 0; next < n; next++) {
                    if ((mask & (1 << next)) != 0) continue;
                    int nextMask = mask | (1 << next);
                    int candidate = dp[mask][last] + distance(point(items.get(last)), point(items.get(next)));
                    if (candidate < dp[nextMask][next]) {
                        dp[nextMask][next] = candidate;
                        parent[nextMask][next] = last;
                    }
                }
            }
        }
        int last = -1;
        int bestDistance = infinity;
        for (int i = 0; i < n; i++) {
            int candidate = dp[masks - 1][i] + distance(point(items.get(i)), PACKING_STATION);
            if (candidate < bestDistance) {
                bestDistance = candidate;
                last = i;
            }
        }
        List<ReservedItemPayload> reversed = new ArrayList<>();
        int mask = masks - 1;
        while (last != -1) {
            reversed.add(items.get(last));
            int previous = parent[mask][last];
            mask ^= 1 << last;
            last = previous;
        }
        java.util.Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private List<ReservedItemPayload> improveTwoOpt(List<ReservedItemPayload> route) {
        // Reversing a leg interval reconnects four endpoints. Keep a change only
        // if its actual walkable A* distance is strictly better.
        boolean improved;
        do {
            improved = false;
            for (int i = 0; i < route.size() - 1 && !improved; i++) {
                for (int j = i + 1; j < route.size(); j++) {
                    GridPoint before = i == 0 ? PACKING_STATION : point(route.get(i - 1));
                    GridPoint start = point(route.get(i));
                    GridPoint end = point(route.get(j));
                    GridPoint after = j == route.size() - 1 ? PACKING_STATION : point(route.get(j + 1));
                    int oldLength = distance(before, start) + distance(end, after);
                    int newLength = distance(before, end) + distance(start, after);
                    if (newLength < oldLength) {
                        for (int left = i, right = j; left < right; left++, right--) {
                            var swap = route.get(left);
                            route.set(left, route.get(right));
                            route.set(right, swap);
                        }
                        improved = true;
                        break;
                    }
                }
            }
        } while (improved);
        return List.copyOf(route);
    }

    private List<ReservedItemPayload> nearestNeighbor(List<ReservedItemPayload> items) {
        List<ReservedItemPayload> remaining = new ArrayList<>(items);
        List<ReservedItemPayload> route = new ArrayList<>(items.size());
        GridPoint current = PACKING_STATION;
        while (!remaining.isEmpty()) {
            int nearest = 0;
            for (int j = 1; j < remaining.size(); j++) {
                int a = distance(current, point(remaining.get(j)));
                int b = distance(current, point(remaining.get(nearest)));
                if (a < b || (a == b && remaining.get(j).sku().compareTo(remaining.get(nearest).sku()) < 0)) {
                    nearest = j;
                }
            }
            ReservedItemPayload next = remaining.remove(nearest);
            route.add(next);
            current = point(next);
        }
        return route;
    }

    public int distanceForOrder(List<ReservedItemPayload> items) {
        GridPoint current = PACKING_STATION;
        int sum = 0;
        for (ReservedItemPayload item : items) {
            GridPoint next = point(item);
            sum += distance(current, next);
            current = next;
        }
        return sum + distance(current, PACKING_STATION);
    }

    private int distance(GridPoint a, GridPoint b) {
        if (a.equals(b)) return 0;
        // Store symmetric distances; paths are undirected on this warehouse grid.
        Leg key = a.x() < b.x() || (a.x() == b.x() && a.y() <= b.y())
                ? new Leg(a, b) : new Leg(b, a);
        return distances.computeIfAbsent(key, ignored -> router.shortestPath(a, b).distance());
    }

    private List<GridPoint> buildPath(List<ReservedItemPayload> items) {
        List<GridPoint> path = new ArrayList<>();
        path.add(PACKING_STATION);
        GridPoint current = PACKING_STATION;
        for (ReservedItemPayload item : items) {
            GridPoint next = point(item);
            append(path, current, next);
            current = next;
        }
        append(path, current, PACKING_STATION);
        return List.copyOf(path);
    }

    private void append(List<GridPoint> path, GridPoint from, GridPoint to) {
        List<GridPoint> segment = router.shortestPath(from, to).path();
        for (int i = 1; i < segment.size(); i++) path.add(segment.get(i));
    }

    private static GridPoint point(ReservedItemPayload item) {
        return new GridPoint(item.x(), item.y());
    }

    private record Leg(GridPoint from, GridPoint to) {}
}
