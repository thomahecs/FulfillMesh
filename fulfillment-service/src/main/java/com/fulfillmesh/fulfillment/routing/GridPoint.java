package com.fulfillmesh.fulfillment.routing;

public record GridPoint(int x, int y) {
    public int manhattan(GridPoint other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
