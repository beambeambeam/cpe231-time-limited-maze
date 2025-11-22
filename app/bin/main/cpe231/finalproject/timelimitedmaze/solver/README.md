# Adding a New Solver

## Steps

1. **Create your solver class** in `cpe231.finalproject.timelimitedmaze.solver` package

   - Extend `MazeSolver`
   - Implement `executeSolve(Maze maze)` - returns the path as `List<Coordinate>`
   - Implement `getAlgorithmName()` - returns a display name

2. **Register your solver** in `SolverRegistry.getAvailableSolvers()`
   - Add `new YourSolver()` to the list

## Example

```java
package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.List;

public final class MySolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "My Algorithm";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    // Your solving logic here
    // Return a path from maze.getStart() to maze.getGoal()
    // Use helper methods: move(), isWalkable(), walkableNeighbors()
  }
}
```

Then in `SolverRegistry.java`:

```java
public static List<MazeSolver> getAvailableSolvers() {
  return List.of(
      new WallFollowerSolver(...),
      new GeneticAlgorithmSolver(),
      new MySolver());  // Add your solver here
}
```

## Helper Methods Available

- `move(Coordinate, Direction)` - Move in a direction
- `move(Coordinate, int deltaRow, int deltaColumn)` - Move by offset
- `isWalkable(Maze, Coordinate)` - Check if cell is walkable
- `isWithinBounds(Maze, Coordinate)` - Check bounds
- `walkableNeighbors(Maze, Coordinate)` - Get all walkable neighbors
- `stepCost(Maze, Coordinate)` - Get cost of a cell
- `Direction` enum - NORTH, EAST, SOUTH, WEST with `left()`, `right()`, `opposite()`

## Notes

- The path must start at `maze.getStart()` and end at `maze.getGoal()`
- Throw `MazeSolvingException` if the maze cannot be solved
- Cost calculation is handled automatically by the base class
