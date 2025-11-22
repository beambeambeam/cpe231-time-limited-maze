package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * This implementation uses a multi-objective approach:
 * 1. Distance-prioritized: Heavily weights reaching the goal with exponential decay
 * 2. Exploration bonus: Rewards paths that visit unique cells
 * 3. Progress tracking: Rewards paths that make progress toward goal
 * 4. Cost minimization: Once goal is reached, focuses on path cost
 */
public final class FitnessCalculator {

  private final Maze maze;
  private final int maxPossibleDistance;

  public FitnessCalculator(Maze maze) {
    this.maze = maze;
    this.maxPossibleDistance = maze.getWidth() + maze.getHeight();
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

    Coordinate end = path.getLast();
    Coordinate goal = maze.getGoal();
    int distanceToGoal = manhattanDistance(end, goal);

    int collisionPenalty = calculateCollisionPenalty(path);
    int pathCost = calculatePathCost(path);

    if (distanceToGoal == 0) {
      double goalReward = 1000000.0;
      double costPenalty = pathCost * 0.1;
      double collisionPenaltyScaled = collisionPenalty * 10.0;
      return goalReward - costPenalty - collisionPenaltyScaled;
    }

    double normalizedDistance = Math.min(distanceToGoal / (double) Math.max(maxPossibleDistance, 1), 1.0);

    double distanceComponent = calculateDistanceComponent(normalizedDistance);

    int uniqueCells = countUniqueCells(path);
    double explorationBonus = calculateExplorationBonus(uniqueCells, path.size());

    double progressBonus = calculateProgressBonus(path, goal);

    double costComponent = 100.0 / (1.0 + pathCost / 1000.0);

    double fitness = distanceComponent + explorationBonus + progressBonus + costComponent - collisionPenalty;

    return fitness;
  }

  private double calculateDistanceComponent(double normalizedDistance) {
    double exponentialDecay = Math.exp(-normalizedDistance * 5.0);
    return 50000.0 * exponentialDecay;
  }

  private double calculateExplorationBonus(int uniqueCells, int pathLength) {
    if (pathLength == 0) {
      return 0.0;
    }
    double uniquenessRatio = uniqueCells / (double) pathLength;
    return 5000.0 * uniquenessRatio;
  }

  private double calculateProgressBonus(List<Coordinate> path, Coordinate goal) {
    if (path.size() < 2) {
      return 0.0;
    }

    Coordinate start = path.getFirst();
    Coordinate end = path.getLast();

    int startDistance = manhattanDistance(start, goal);
    int endDistance = manhattanDistance(end, goal);

    if (startDistance == 0) {
      return 0.0;
    }

    double progressRatio = (startDistance - endDistance) / (double) startDistance;
    return 10000.0 * Math.max(0.0, progressRatio);
  }

  private int countUniqueCells(List<Coordinate> path) {
    Set<Coordinate> unique = new HashSet<>(path);
    return unique.size();
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
