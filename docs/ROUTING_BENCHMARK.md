# Routing benchmark

The route test uses the same coordinates as the seeded inventory data and the default 20 x 20 warehouse grid.

Baseline behavior: visit items in the order received, using A* between each consecutive stop.

Optimized behavior: choose the remaining item with the shortest A* path from the current position, then return to the packing station.

Fixed benchmark order:

```text
SKU-A -> SKU-B -> SKU-C -> SKU-D -> SKU-E -> SKU-F -> SKU-G -> SKU-H
```

Expected test result:

```text
sequential distance = 102 grid cells
optimized distance  = 80 grid cells
reduction           = 21.5686%
rounded reduction   = 22%
```

Run only the benchmark test:

```bash
mvn -pl fulfillment-service -am -Dtest=RoutingBenchmarkTest test
```

The test asserts the 102 -> 80 result, so a routing change that breaks the resume number is visible immediately.
