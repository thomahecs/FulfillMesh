package com.fulfillmesh.fulfillment.routing;

import java.util.*;

public class AStarRouter {
    private final WarehouseGrid grid;

    public AStarRouter(WarehouseGrid grid) {
        this.grid = grid;
    }

    public PathResult shortestPath(GridPoint start, GridPoint goal) {
        if (!grid.canWalk(start) || !grid.canWalk(goal)) {
            throw new IllegalArgumentException("Start and goal must be walkable");
        }

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingInt(Node::f)
                        .thenComparingInt(node -> node.point().x())
                        .thenComparingInt(node -> node.point().y())
        );
        Map<GridPoint, Integer> gScore = new HashMap<>();
        Map<GridPoint, GridPoint> cameFrom = new HashMap<>();

        gScore.put(start, 0);
        open.add(new Node(start, 0, start.manhattan(goal)));

        while (!open.isEmpty()) {
            Node current = open.poll();

            // A point can appear more than once in the queue. Ignore an old entry
            // if a shorter route to that point has already been discovered.
            if (current.g() != gScore.getOrDefault(current.point(), Integer.MAX_VALUE)) {
                continue;
            }

            if (current.point().equals(goal)) {
                return new PathResult(reconstruct(cameFrom, goal), current.g());
            }

            for (GridPoint next : neighbors(current.point())) {
                int tentative = current.g() + 1;
                if (tentative < gScore.getOrDefault(next, Integer.MAX_VALUE)) {
                    cameFrom.put(next, current.point());
                    gScore.put(next, tentative);
                    open.add(new Node(next, tentative, tentative + next.manhattan(goal)));
                }
            }
        }

        throw new IllegalStateException("No route exists between " + start + " and " + goal);
    }

    private List<GridPoint> neighbors(GridPoint p) {
        List<GridPoint> candidates = List.of(
                new GridPoint(p.x() + 1, p.y()),
                new GridPoint(p.x() - 1, p.y()),
                new GridPoint(p.x(), p.y() + 1),
                new GridPoint(p.x(), p.y() - 1)
        );
        return candidates.stream().filter(grid::canWalk).toList();
    }

    private List<GridPoint> reconstruct(Map<GridPoint, GridPoint> cameFrom, GridPoint goal) {
        LinkedList<GridPoint> path = new LinkedList<>();
        GridPoint current = goal;
        path.addFirst(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.addFirst(current);
        }
        return List.copyOf(path);
    }

    private record Node(GridPoint point, int g, int f) {
    }
}
