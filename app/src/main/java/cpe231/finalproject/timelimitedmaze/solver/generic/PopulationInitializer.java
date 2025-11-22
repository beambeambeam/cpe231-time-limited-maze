package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Initializes the initial population using Ramped Half-and-Half strategy.
 *
 * Population initialization is critical in genetic algorithms - it provides the
 * starting genetic diversity. Ramped Half-and-Half creates a mix of:
 * - Random walk paths (exploration, diverse structures)
 * - Biased DFS paths (exploitation, goal-oriented)
 *
 * This balanced approach helps the algorithm explore the solution space while
 * also having some individuals that are already somewhat directed toward the
 * goal.
 */
public final class PopulationInitializer {

  private final Random random;

  public PopulationInitializer(Random random) {
    this.random = random;
  }

  /**
   * Creates an initial population using ramped half-and-half initialization.
   *
   * @param maze           The maze to solve
   * @param populationSize Size of population to create
   * @return List of initialized path chromosomes
   */
  public List<PathChromosome> initialize(Maze maze, int populationSize) {
    List<PathChromosome> population = new ArrayList<>();
    int halfSize = populationSize / 2;
    int manhattanDist = manhattanDistance(maze.getStart(), maze.getGoal());
    int maxLength = 2 * manhattanDist;

    for (int i = 0; i < halfSize; i++) {
      List<Coordinate> randomWalkPath = createRandomWalk(maze, maxLength);
      population.add(new PathChromosome(randomWalkPath, maze));
    }

    for (int i = 0; i < populationSize - halfSize; i++) {
      List<Coordinate> biasedDfsPath = createBiasedDfs(maze, maxLength);
      population.add(new PathChromosome(biasedDfsPath, maze));
    }

    return population;
  }

  /**
   * Creates a path using random walk from start.
   * Randomly selects walkable neighbors until max length or goal reached.
   */
  private List<Coordinate> createRandomWalk(Maze maze, int maxLength) {
    List<Coordinate> path = new ArrayList<>();
    Coordinate current = maze.getStart();
    path.add(current);
    Coordinate goal = maze.getGoal();

    for (int step = 0; step < maxLength; step++) {
      if (current.equals(goal)) {
        break;
      }

      List<Coordinate> neighbors = getWalkableNeighbors(maze, current);
      if (neighbors.isEmpty()) {
        break;
      }

      current = neighbors.get(random.nextInt(neighbors.size()));
      path.add(current);
    }

    return path;
  }

  /**
   * Creates a path using randomized biased DFS.
   * Prefers neighbors that minimize Manhattan distance to goal, but with
   * randomness.
   */
  private List<Coordinate> createBiasedDfs(Maze maze, int maxLength) {
    List<Coordinate> path = new ArrayList<>();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    path.add(start);

    Coordinate current = start;
    java.util.Set<Coordinate> visited = new java.util.HashSet<>();
    visited.add(current);

    for (int step = 0; step < maxLength; step++) {
      if (current.equals(goal)) {
        break;
      }

      List<Coordinate> neighbors = getWalkableNeighbors(maze, current);
      neighbors.removeAll(visited);

      if (neighbors.isEmpty()) {
        break;
      }

      neighbors.sort((a, b) -> {
        int distA = manhattanDistance(a, goal);
        int distB = manhattanDistance(b, goal);
        if (distA != distB) {
          return Integer.compare(distA, distB);
        }
        return random.nextInt(3) - 1;
      });

      int selectionIndex = random.nextInt(Math.min(neighbors.size(), 3));
      current = neighbors.get(selectionIndex);
      path.add(current);
      visited.add(current);
    }

    return path;
  }

  private List<Coordinate> getWalkableNeighbors(Maze maze, Coordinate coord) {
    List<Coordinate> neighbors = new ArrayList<>();

    int[][] deltas = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    for (int[] delta : deltas) {
      int newRow = coord.row() + delta[0];
      int newCol = coord.column() + delta[1];

      if (newRow >= 0 && newRow < maze.getHeight() &&
          newCol >= 0 && newCol < maze.getWidth()) {
        Coordinate neighbor = new Coordinate(newRow, newCol);
        if (maze.getCell(neighbor).isWalkable()) {
          neighbors.add(neighbor);
        }
      }
    }

    return neighbors;
  }

  private int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column());
  }
}
