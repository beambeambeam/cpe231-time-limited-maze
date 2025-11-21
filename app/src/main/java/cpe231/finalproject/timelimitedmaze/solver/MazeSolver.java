package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MazeSolver {

  public final SolverResult solve(Maze maze) {
    Objects.requireNonNull(maze, "maze cannot be null");
    long startTimeNs = System.nanoTime();
    List<Coordinate> path = executeSolve(maze);
    long endTimeNs = System.nanoTime();
    int totalCost = calculatePathCost(maze, path);
    return new SolverResult(List.copyOf(path), totalCost, startTimeNs, endTimeNs);
  }

  protected abstract List<Coordinate> executeSolve(Maze maze);

  public abstract String getAlgorithmName();

  protected final Coordinate move(Coordinate coordinate, int deltaRow, int deltaColumn) {
    Objects.requireNonNull(coordinate, "coordinate cannot be null");
    return new Coordinate(coordinate.row() + deltaRow, coordinate.column() + deltaColumn);
  }

  protected final Coordinate move(Coordinate coordinate, Direction direction) {
    return move(coordinate, direction.deltaRow, direction.deltaColumn);
  }

  public final int calculatePathCost(Maze maze, List<Coordinate> path) {
    Objects.requireNonNull(maze, "maze cannot be null");
    Objects.requireNonNull(path, "path cannot be null");
    int total = 0;
    for (Coordinate coordinate : path) {
      total += stepCost(maze, coordinate);
    }
    return total;
  }

  protected final int stepCost(Maze maze, Coordinate coordinate) {
    Objects.requireNonNull(maze, "maze cannot be null");
    Objects.requireNonNull(coordinate, "coordinate cannot be null");
    if (!isWithinBounds(maze, coordinate)) {
      throw new IllegalArgumentException("Coordinate " + coordinate + " is outside maze bounds");
    }
    MazeCell cell = maze.getCell(coordinate);
    if (!cell.isWalkable()) {
      throw new IllegalArgumentException("Coordinate " + coordinate + " is not walkable");
    }
    return cell.stepCost();
  }

  protected final boolean isWalkable(Maze maze, Coordinate coordinate) {
    return isWithinBounds(maze, coordinate) && maze.getCell(coordinate).isWalkable();
  }

  protected final boolean isWithinBounds(Maze maze, Coordinate coordinate) {
    if (coordinate.row() < 0 || coordinate.row() >= maze.getHeight()) {
      return false;
    }
    if (coordinate.column() < 0 || coordinate.column() >= maze.getWidth()) {
      return false;
    }
    return true;
  }

  protected final List<Coordinate> walkableNeighbors(Maze maze, Coordinate origin) {
    Objects.requireNonNull(maze, "maze cannot be null");
    Objects.requireNonNull(origin, "origin cannot be null");
    List<Coordinate> neighbors = new ArrayList<>();
    for (Direction direction : Direction.values()) {
      Coordinate candidate = move(origin, direction);
      if (isWalkable(maze, candidate)) {
        neighbors.add(candidate);
      }
    }
    return neighbors;
  }

  protected enum Direction {
    NORTH(-1, 0),
    EAST(0, 1),
    SOUTH(1, 0),
    WEST(0, -1);

    private final int deltaRow;
    private final int deltaColumn;

    Direction(int deltaRow, int deltaColumn) {
      this.deltaRow = deltaRow;
      this.deltaColumn = deltaColumn;
    }

    public Direction left() {
      return switch (this) {
        case NORTH -> WEST;
        case WEST -> SOUTH;
        case SOUTH -> EAST;
        case EAST -> NORTH;
      };
    }

    public Direction right() {
      return switch (this) {
        case NORTH -> EAST;
        case EAST -> SOUTH;
        case SOUTH -> WEST;
        case WEST -> NORTH;
      };
    }

    public Direction opposite() {
      return switch (this) {
        case NORTH -> SOUTH;
        case SOUTH -> NORTH;
        case EAST -> WEST;
        case WEST -> EAST;
      };
    }
  }
}
