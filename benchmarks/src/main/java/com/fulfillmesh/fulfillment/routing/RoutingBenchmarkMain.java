package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.ReservedItemPayload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Reproducible 10k-order experiment; no Spring, Docker, Maven or external dependencies. */
public final class RoutingBenchmarkMain {
    private static final long SEED = 20261016L;
    private static final int COUNT = 10_000;
    private RoutingBenchmarkMain() {}

    public static void main(String[] args) throws IOException {
        WarehouseGrid grid = WarehouseGrid.defaultLayout();
        PickingRoutePlanner planner = new PickingRoutePlanner(new AStarRouter(grid));
        List<GridPoint> locations = new ArrayList<>();
        for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) {
            GridPoint p = new GridPoint(x, y);
            if (grid.canWalk(p) && (x != 0 || y != 0)) locations.add(p);
        }
        Random random = new Random(SEED);
        long sequential = 0, nearest = 0, optimized = 0;
        long improvedNearest = 0, improvedSequential = 0;
        long started = System.nanoTime();
        for (int orderId = 0; orderId < COUNT; orderId++) {
            Collections.shuffle(locations, random);
            int size = 18 + random.nextInt(5); // 18-22 distinct SKU locations
            List<ReservedItemPayload> order = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                GridPoint p = locations.get(j);
                order.add(new ReservedItemPayload("SKU-" + j, 1, p.x(), p.y()));
            }
            // The sequential baseline represents aisle-sorted pick lists, not a
            // deliberately shuffled adversarial route. Ties retain SKU order.
            order.sort(Comparator.comparingInt(ReservedItemPayload::x)
                    .thenComparingInt(ReservedItemPayload::y));
            int nn = planner.nearestNeighborDistance(order);
            RoutePlan result = planner.plan(order);
            if (result.optimizedDistance() > nn || result.optimizedDistance() > result.sequentialDistance()) {
                throw new AssertionError("Route optimizer regressed on order " + orderId + " nn=" + nn + " opt=" + result.optimizedDistance() + " seq=" + result.sequentialDistance() + " order=" + order + " optOrder=" + planner.optimizedOrder(order) + " calculated=" + planner.distanceForOrder(planner.optimizedOrder(order)));
            }
            sequential += result.sequentialDistance();
            nearest += nn;
            optimized += result.optimizedDistance();
            if (result.optimizedDistance() < nn) improvedNearest++;
            if (result.optimizedDistance() < result.sequentialDistance()) improvedSequential++;
        }
        double vsNearest = nearest == 0 ? 0 : 100.0 * (nearest - optimized) / nearest;
        double vsSequential = sequential == 0 ? 0 : 100.0 * (sequential - optimized) / sequential;
        long runtimeMs = (System.nanoTime() - started) / 1_000_000;
        String json = String.format(Locale.ROOT,
                "{\n  \"experiment\": \"A* shortest paths plus Held-Karp (up to 10 picks), 2-opt fallback\",\n" +
                        "  \"generator\": \"18-22 distinct walkable cells; aisle-sorted sequential baseline\",\n" +
                        "  \"seed\": %d,\n  \"orders\": %d,\n" +
                        "  \"sequential_total_steps\": %d,\n  \"nearest_neighbor_total_steps\": %d,\n" +
                        "  \"optimized_total_steps\": %d,\n  \"reduction_vs_nearest_percent\": %.4f,\n" +
                        "  \"reduction_vs_sequential_percent\": %.4f,\n" +
                        "  \"orders_improved_vs_nearest\": %d,\n  \"orders_improved_vs_sequential\": %d,\n" +
                        "  \"runtime_ms\": %d\n}\n",
                SEED, COUNT, sequential, nearest, optimized, vsNearest, vsSequential,
                improvedNearest, improvedSequential, runtimeMs);
        System.out.print(json);
        if (args.length > 0) {
            Path output = Path.of(args[0]);
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, json);
        }
        if (vsNearest < 12.0 || vsSequential < 22.0) {
            throw new AssertionError("Benchmark fell below the documented route improvement thresholds");
        }
    }
}
