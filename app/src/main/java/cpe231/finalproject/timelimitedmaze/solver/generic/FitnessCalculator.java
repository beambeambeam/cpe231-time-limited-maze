package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.List;

/**
 * Calculates fitness for path chromosomes in the genetic algorithm.
 *
 * Fitness is the measure of how "good" a solution is. In evolutionary
 * algorithms,
 * higher fitness means better solutions. This drives selection pressure -
 * individuals
 * with higher fitness are more likely to be selected as parents, passing their
 * genetic material to the next generation.
 *
 * This implementation uses a multi-objective approach that switches between:
 * 1. Distance-prioritized mode: Heavily weights reaching the goal (exploration
 * phase)
 * 2. Cost-prioritized mode: Focuses on minimizing path cost (exploitation
 * phase)
 *
 * The formula from the specification:
 * F(P) = (1000 / (1 + Cost(P))) × 0.5 + (1000 / (1 + Dist(P_end, Goal))) × 2.0
 * - CollisionPenalty
 */
public final class FitnessCalculator {

  private final Maze maze;
  private boolean goalReached = false;

  public FitnessCalculator(Maze maze) {
    this.maze = maze;
  }

  /**
   * Calculates fitness for a path chromosome.
   *
   * @param chromosome The path chromosome to evaluate
   * @return Fitness score (higher is better)
   */
  public double calculateFitness(PathChromosome chromosome) {
    List<Coordinate> path = chromosome.getPath();

    if (path.isEmpty()) {
      return Double.NEGATIVE_INFINITY;
    }

    int pathCost = calculatePathCost(path);
    Coordinate end = path.getLast();
    Coordinate goal = maze.getGoal();

    int distanceToGoal = manhattanDistance(end, goal);

    if (distanceToGoal == 0) {
      goalReached = true;
    }

    int collisionPenalty = calculateCollisionPenalty(path);

    if (goalReached) {
      return 100000.0 - pathCost - collisionPenalty;
    }

    int maxDistance = maze.getWidth() + maze.getHeight();
    double normalizedDistance = distanceToGoal / (double) Math.max(maxDistance, 1);

    double distanceComponent = 10000.0 * (1.0 - normalizedDistance);
    double costComponent = 100.0 / (1.0 + pathCost / 100.0);

    double fitness = distanceComponent + costComponent - collisionPenalty;

    return fitness;
  }

  /**
   * Checks if any path has reached the goal, switching to cost-prioritized mode.
   */
  public boolean hasGoalBeenReached() {
    return goalReached;
  }

  /**
   * Resets the goal reached flag (for new runs).
   */
  public void reset() {
    goalReached = false;
  }

  private int calculatePathCost(List<Coordinate> path) {
    int total = 0;
    for (Coordinate coord : path) {
      if (isValidCoordinate(coord)) {
        total += maze.getCell(coord).stepCost();
      } else {
        total += 1000;
      }
    }
    return total;
  }

  private int calculateCollisionPenalty(List<Coordinate> path) {
    int penalty = 0;

    for (int i = 0; i < path.size(); i++) {
      Coordinate coord = path.get(i);

      if (!isValidCoordinate(coord)) {
        penalty += 1000;
        continue;
      }

      if (!maze.getCell(coord).isWalkable()) {
        penalty += 1000;
        continue;
      }

      if (i > 0) {
        Coordinate prev = path.get(i - 1);
        if (!isAdjacent(prev, coord)) {
          penalty += 500;
        }
      }
    }

    return penalty;
  }

  private boolean isValidCoordinate(Coordinate coord) {
    return coord.row() >= 0 && coord.row() < maze.getHeight() &&
        coord.column() >= 0 && coord.column() < maze.getWidth();
  }

  private boolean isAdjacent(Coordinate a, Coordinate b) {
    int rowDiff = Math.abs(a.row() - b.row());
    int colDiff = Math.abs(a.column() - b.column());
    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
  }

  private int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column());
  }
}
