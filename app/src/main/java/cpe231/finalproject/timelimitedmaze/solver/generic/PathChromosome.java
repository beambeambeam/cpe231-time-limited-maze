package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a path through a maze as a chromosome in the genetic algorithm.
 *
 * In genetic algorithms, a chromosome is the encoded representation of a
 * candidate solution.
 * For maze solving, each chromosome is a sequence of coordinates (genes)
 * forming a path
 * from start to goal. The variable-length nature allows paths to expand or
 * contract during
 * evolution, enabling the algorithm to find paths of different lengths.
 *
 * This class wraps a List<Coordinate> which serves as the genetic material (the
 * genes).
 * Each coordinate in the path is a gene, and the entire sequence forms the
 * chromosome.
 */
public final class PathChromosome {

  private final List<Coordinate> path;
  private final Maze maze;

  public PathChromosome(List<Coordinate> path, Maze maze) {
    this.path = new ArrayList<>(Objects.requireNonNull(path, "path cannot be null"));
    this.maze = Objects.requireNonNull(maze, "maze cannot be null");
  }

  public List<Coordinate> getPath() {
    return List.copyOf(path);
  }

  public Maze getMaze() {
    return maze;
  }

  public int size() {
    return path.size();
  }

  public boolean isEmpty() {
    return path.isEmpty();
  }

  public Coordinate getFirst() {
    if (path.isEmpty()) {
      throw new IllegalStateException("Path is empty");
    }
    return path.getFirst();
  }

  public Coordinate getLast() {
    if (path.isEmpty()) {
      throw new IllegalStateException("Path is empty");
    }
    return path.getLast();
  }

  public Coordinate get(int index) {
    return path.get(index);
  }

  /**
   * Creates a copy of this chromosome with a modified path.
   * Used during crossover and mutation operations to create new individuals.
   */
  public PathChromosome withPath(List<Coordinate> newPath) {
    return new PathChromosome(newPath, maze);
  }

  /**
   * Validates that the path is structurally valid for the maze.
   * Checks that consecutive coordinates are adjacent and walkable.
   */
  public boolean isValid() {
    if (path.isEmpty()) {
      return false;
    }

    Coordinate start = maze.getStart();
    if (!path.getFirst().equals(start)) {
      return false;
    }

    for (int i = 0; i < path.size() - 1; i++) {
      Coordinate current = path.get(i);
      Coordinate next = path.get(i + 1);

      if (!isAdjacent(current, next)) {
        return false;
      }

      if (!isWalkable(current) || !isWalkable(next)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Repairs the path to ensure it starts at the maze start and is connected.
   * If the path doesn't start at start, prepends a path from start to the first
   * coordinate.
   * Ensures all consecutive coordinates are adjacent.
   */
  public PathChromosome repair() {
    List<Coordinate> repaired = new ArrayList<>();
    Coordinate start = maze.getStart();

    if (path.isEmpty() || !path.getFirst().equals(start)) {
      repaired.add(start);
    } else {
      repaired.add(path.getFirst());
    }

    for (int i = 1; i < path.size(); i++) {
      Coordinate current = repaired.getLast();
      Coordinate target = path.get(i);

      if (isAdjacent(current, target) && isWalkable(target)) {
        repaired.add(target);
      } else {
        List<Coordinate> bridge = findBridgePath(current, target);
        if (!bridge.isEmpty()) {
          repaired.addAll(bridge.subList(1, bridge.size()));
        } else {
          List<Coordinate> fallback = findNearestWalkable(target);
          if (!fallback.isEmpty()) {
            repaired.addAll(fallback);
          }
        }
      }
    }

    if (repaired.size() < 2) {
      repaired.add(start);
    }

    PathChromosome result = new PathChromosome(repaired, maze);
    PathChromosome extended = result.extendTowardGoal();

    if (extended.size() < 2) {
      List<Coordinate> minimalPath = new ArrayList<>();
      minimalPath.add(start);
      List<Coordinate> neighbors = getWalkableNeighbors(start);
      if (!neighbors.isEmpty()) {
        minimalPath.add(neighbors.get(0));
      } else {
        minimalPath.add(start);
      }
      return new PathChromosome(minimalPath, maze).extendTowardGoal();
    }

    return extended;
  }

  /**
   * Extends the path toward the goal if it's too short or doesn't reach the goal.
   * This helps ensure paths grow and explore the maze.
   */
  public PathChromosome extendTowardGoal() {
    if (path.isEmpty()) {
      List<Coordinate> newPath = new ArrayList<>();
      newPath.add(maze.getStart());
      return new PathChromosome(newPath, maze).extendTowardGoal();
    }

    Coordinate goal = maze.getGoal();
    Coordinate current = path.getLast();

    if (current.equals(goal)) {
      return this;
    }

    int manhattanDist = manhattanDistance(current, goal);
    int minDesiredLength = Math.max(manhattanDist * 3, 50);
    minDesiredLength = Math.min(minDesiredLength, 500);

    if (path.size() < minDesiredLength) {
      List<Coordinate> extended = new ArrayList<>(path);
      Coordinate last = extended.getLast();
      int stepsToAdd = minDesiredLength - path.size();
      java.util.Set<Coordinate> visited = new java.util.HashSet<>(extended);

      for (int i = 0; i < stepsToAdd && !last.equals(goal); i++) {
        List<Coordinate> neighbors = getWalkableNeighbors(last);
        neighbors.removeAll(visited);

        if (neighbors.isEmpty()) {
          neighbors = getWalkableNeighbors(last);
          if (neighbors.isEmpty()) {
            break;
          }
        }

        neighbors.sort((a, b) -> {
          int distA = manhattanDistance(a, goal);
          int distB = manhattanDistance(b, goal);
          if (distA != distB) {
            return Integer.compare(distA, distB);
          }
          return 0;
        });

        last = neighbors.get(0);
        extended.add(last);
        visited.add(last);
      }

      return new PathChromosome(extended, maze);
    }

    return this;
  }

  private List<Coordinate> getWalkableNeighbors(Coordinate coord) {
    List<Coordinate> neighbors = new ArrayList<>();
    int[][] deltas = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    for (int[] delta : deltas) {
      int newRow = coord.row() + delta[0];
      int newCol = coord.column() + delta[1];
      if (newRow >= 0 && newRow < maze.getHeight() &&
          newCol >= 0 && newCol < maze.getWidth()) {
        Coordinate neighbor = new Coordinate(newRow, newCol);
        if (isWalkable(neighbor)) {
          neighbors.add(neighbor);
        }
      }
    }
    return neighbors;
  }

  private int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column());
  }

  private List<Coordinate> findBridgePath(Coordinate from, Coordinate to) {
    int maxDepth = Math.min(100, maze.getWidth() * maze.getHeight() / 4);
    List<Coordinate> aStarPath = AStarHelper.findPath(maze, from, to, maxDepth);
    if (!aStarPath.isEmpty()) {
      return aStarPath;
    }

    List<Coordinate> simplePath = findSimplePath(from, to);
    if (!simplePath.isEmpty()) {
      return simplePath;
    }

    return new ArrayList<>();
  }

  private List<Coordinate> findNearestWalkable(Coordinate target) {
    if (isWalkable(target)) {
      return List.of(target);
    }

    for (int radius = 1; radius <= 5; radius++) {
      for (int dr = -radius; dr <= radius; dr++) {
        for (int dc = -radius; dc <= radius; dc++) {
          if (Math.abs(dr) + Math.abs(dc) != radius) {
            continue;
          }
          int newRow = target.row() + dr;
          int newCol = target.column() + dc;
          if (newRow >= 0 && newRow < maze.getHeight() &&
              newCol >= 0 && newCol < maze.getWidth()) {
            Coordinate candidate = new Coordinate(newRow, newCol);
            if (isWalkable(candidate)) {
              return List.of(candidate);
            }
          }
        }
      }
    }

    return new ArrayList<>();
  }

  private boolean isAdjacent(Coordinate a, Coordinate b) {
    int rowDiff = Math.abs(a.row() - b.row());
    int colDiff = Math.abs(a.column() - b.column());
    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
  }

  private boolean isWalkable(Coordinate coord) {
    if (coord.row() < 0 || coord.row() >= maze.getHeight()) {
      return false;
    }
    if (coord.column() < 0 || coord.column() >= maze.getWidth()) {
      return false;
    }
    return maze.getCell(coord).isWalkable();
  }

  private List<Coordinate> findSimplePath(Coordinate from, Coordinate to) {
    List<Coordinate> simplePath = new ArrayList<>();
    simplePath.add(from);

    int currentRow = from.row();
    int currentCol = from.column();
    int targetRow = to.row();
    int targetCol = to.column();

    while (currentRow != targetRow || currentCol != targetCol) {
      if (currentRow < targetRow) {
        currentRow++;
      } else if (currentRow > targetRow) {
        currentRow--;
      } else if (currentCol < targetCol) {
        currentCol++;
      } else if (currentCol > targetCol) {
        currentCol--;
      }

      Coordinate next = new Coordinate(currentRow, currentCol);
      if (!isWalkable(next)) {
        return new ArrayList<>();
      }
      simplePath.add(next);
    }

    return simplePath;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    PathChromosome that = (PathChromosome) other;
    return path.equals(that.path) && maze.equals(that.maze);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, maze);
  }

  @Override
  public String toString() {
    return "PathChromosome{size=" + path.size() + ", startsAt=" +
        (path.isEmpty() ? "empty" : path.getFirst()) +
        ", endsAt=" + (path.isEmpty() ? "empty" : path.getLast()) + "}";
  }
}
