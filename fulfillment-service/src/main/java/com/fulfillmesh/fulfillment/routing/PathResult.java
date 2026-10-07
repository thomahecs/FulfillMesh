package com.fulfillmesh.fulfillment.routing;

import java.util.List;

public record PathResult(List<GridPoint> path, int distance) {
}
