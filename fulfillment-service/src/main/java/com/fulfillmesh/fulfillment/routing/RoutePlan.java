package com.fulfillmesh.fulfillment.routing;

import com.fulfillmesh.common.events.PickStopPayload;

import java.util.List;

public record RoutePlan(
        List<PickStopPayload> optimizedStops,
        List<GridPoint> optimizedPath,
        int optimizedDistance,
        int sequentialDistance,
        double improvementPercent
) {
}
