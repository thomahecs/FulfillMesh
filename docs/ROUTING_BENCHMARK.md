# Warehouse routing experiment

## Purpose and reproducibility

Run `./scripts/benchmark-routing.sh` with JDK 21. The benchmark compiles only
pure Java classes and does not need Maven, a network connection, or Docker.
It writes `reports/routing-benchmark.json`.

The warehouse is a **20 by 20** grid with five shelf columns and walkable cross
aisles. A* finds obstacle-aware shortest walking paths, starting and ending at
packing station `(0, 0)`.

The experiment uses 10,000 pseudo-random orders, each with **18–22 distinct
walkable picking locations**. A single `java.util.Random` seeded with
`20261016` selects locations. Baselines and algorithm receive **the same
orders**:

- **Sequential:** visits the locations in ascending `(x, y)` order. This
  models an aisle-sorted list; it is not an artificially randomized baseline.
- **Nearest-neighbor:** at each step visits the pick having the shortest *A*
  walking distance from its current location; ties use SKU order.
- **Optimized:** for up to 10 locations, computes the exact shortest closed
  tour with Held–Karp dynamic programming over A* leg distances. For larger
  tours, starts from both the nearest-neighbor tour and the sequential tour,
  uses 2-opt edge reversals until no improvement remains, and keeps the
  shorter result. (The 18–22-location benchmark uses this 2-opt fallback.)

For consistency, all distances include the return trip to packing. Reported
reductions use **sum of distances over all 10,000 orders**, not a mean of
per-order percentages. These are simulated grid steps, not physical warehouse
measurements or observed picker productivity.

## Results from the offline Java run in this handoff

| Metric | Observed |
|---|---:|
| Seed | 20261016 |
| Orders | 10,000 |
| Sequential distance, total grid steps | 1,865,602 |
| Nearest-neighbor distance, total grid steps | 1,255,014 |
| Optimized distance, total grid steps | 1,098,176 |
| Reduction vs nearest-neighbor | **12.4969%** |
| Reduction vs sequential | **41.1356%** |
| Orders improved vs nearest-neighbor | 9,436 |
| Orders improved vs sequential | 10,000 |

The old README benchmark still has a single, fixed 8-item order with
**102 sequential vs 80 optimized grid steps** (21.57% reduction). That
individual example is a unit test and **not** the 10,000-order aggregate.

The new aggregate reductions exceed the resume's conservative 12% and 22%
figures, respectively. **This is evidence for those thresholds under the
specific simulation described above, not a claim that the exact values hold
for any order distribution.**
