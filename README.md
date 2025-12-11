Sawasdekub

to run this program please use ./gradlew run

## Maze GUI

Launch the interactive Raylib GUI to visualize and run solvers:

```bash
./gradlew :app:run
```

- Opens with `m100_100.txt` loaded; mazes come from `app/src/main/resources/maze/`.
- Use the "Select Algorithm..." dropdown to pick a solver; it runs immediately and draws the path plus cost, path length, and execution time.
- Use the "Select Maze..." dropdown to switch mazes; valid files are highlighted and automatically re-solved when an algorithm is selected.
- The right panel shows solver stats, legend, and error messages if a maze cannot be solved.
- Press `Esc` to close the window. macOS first-thread handling is already configured in the Gradle `run` task.

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
