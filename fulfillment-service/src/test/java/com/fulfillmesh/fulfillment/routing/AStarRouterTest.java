package com.fulfillmesh.fulfillment.routing;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AStarRouterTest {

    @Test
    void routesAroundObstacle() {
        WarehouseGrid grid = new WarehouseGrid(
                5,
                5,
                Set.of(new GridPoint(1, 0), new GridPoint(1, 1), new GridPoint(1, 2))
        );
        AStarRouter router = new AStarRouter(grid);

        PathResult result = router.shortestPath(new GridPoint(0, 0), new GridPoint(2, 0));

        assertEquals(new GridPoint(0, 0), result.path().getFirst());
        assertEquals(new GridPoint(2, 0), result.path().getLast());
        assertTrue(result.distance() > 2);
    }

    @Test
    void rejectsBlockedGoal() {
        WarehouseGrid grid = new WarehouseGrid(3, 3, Set.of(new GridPoint(2, 2)));
        AStarRouter router = new AStarRouter(grid);

        assertThrows(
                IllegalArgumentException.class,
                () -> router.shortestPath(new GridPoint(0, 0), new GridPoint(2, 2))
        );
    }
}
