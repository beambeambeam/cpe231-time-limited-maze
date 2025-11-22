Sawasdekub

to run this program please use ./gradlew run

## Genetic Algorithm Pre-computation

Pre-compute solutions for the genetic algorithm solver on all mazes:

```bash
./gradlew :app:generic
```

This will run the GA on all available mazes and display results in a table format. The GA will evolve through multiple generations internally until it finds a solution. Pre-computed solutions are cached in `ga_checkpoints/` and will be used automatically by the GUI solver.

## Profiler

Profile solvers on mazes and view results in a table:

```bash
# Profile all solvers on all mazes
./gradlew :app:profiler

# List available algorithms and mazes
./gradlew :app:profiler -Pargs="--list"

# Profile specific algorithm(s) on specific maze(s)
./gradlew :app:profiler -Pargs="--algo 'Genetic Algorithm' --maze m15_15.txt"

# Profile multiple algorithms on multiple mazes
./gradlew :app:profiler -Pargs="-a 'Wall Follower (LEFT)' -a 'Wall Follower (RIGHT)' -m m30_30.txt -m m40_40.txt"
```

Options:

- `-a, --algo <name>`: Select algorithm(s) to profile (can specify multiple)
- `-m, --maze <name>`: Select maze(s) to profile (can specify multiple)
- `-l, --list`: List available algorithms and mazes
- `-h, --help`: Show help message

The profiler displays:

- Results table with cost, path length, execution time, and goal status
- Summary statistics for each solver (success rate, total time, average cost)
