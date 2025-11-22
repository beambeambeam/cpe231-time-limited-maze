Sawasdekub

to run this program please use ./gradlew run

## Genetic Algorithm Pre-computation

Pre-compute solutions for the genetic algorithm solver on all mazes:

```bash
./gradlew :app:generic
```

This will run the GA on all available mazes and display results in a table format. The GA will evolve through multiple generations internally until it finds a solution. Pre-computed solutions are cached in `ga_checkpoints/` and will be used automatically by the GUI solver.

## Profiler

Profile all solvers on all mazes and view results in a table:

```bash
./gradlew :app:profiler
```

This will run every algorithm on every maze and display:

- Results table with cost, path length, execution time, and goal status
- Summary statistics for each solver (success rate, total time, average cost)
