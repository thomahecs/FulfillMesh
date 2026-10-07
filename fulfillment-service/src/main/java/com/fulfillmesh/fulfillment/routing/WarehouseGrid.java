package com.fulfillmesh.fulfillment.routing;

import java.util.HashSet;
import java.util.Set;

public final class WarehouseGrid {
    private final int width;
    private final int height;
    private final Set<GridPoint> blocked;

    public WarehouseGrid(int width, int height, Set<GridPoint> blocked) {
        this.width = width;
        this.height = height;
        this.blocked = Set.copyOf(blocked);
    }

    public boolean canWalk(GridPoint point) {
        return point.x() >= 0
                && point.y() >= 0
                && point.x() < width
                && point.y() < height
                && !blocked.contains(point);
    }

    public static WarehouseGrid defaultLayout() {
        Set<GridPoint> shelves = new HashSet<>();

        // Five long shelf rows are represented as blocked vertical strips.
        // Cross aisles at y=4, 9, and 14 keep the grid connected.
        for (int x : new int[]{3, 6, 9, 12, 15}) {
            for (int y = 1; y < 19; y++) {
                if (y != 4 && y != 9 && y != 14) {
                    shelves.add(new GridPoint(x, y));
                }
            }
        }
        return new WarehouseGrid(20, 20, shelves);
    }
}
