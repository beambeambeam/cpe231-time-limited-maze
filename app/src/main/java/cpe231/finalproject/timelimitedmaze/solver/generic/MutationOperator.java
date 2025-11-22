package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mutation operators for genetic algorithm.
 *
 * Mutation introduces random variations into chromosomes, maintaining genetic
 * diversity
 * and enabling exploration of new regions in the solution space. Without
 * mutation,
 * the algorithm might converge prematurely to local optima.
 *
 * Three mutation strategies are implemented:
 * 1. Perturbation (10%): Moves a random waypoint to a neighboring cell
 * 2. Smoothing (20%): Checks line-of-sight and short-circuits paths when
 * possible
 * 3. Regrowth (5%): Deletes a segment and uses A* to bridge the gap
 */
public final class MutationOperator {

  private static final double PERTURBATION_RATE = 0.10;
  private static final double SMOOTHING_RATE = 0.20;
  private static final double REGROWTH_RATE = 0.05;

  private final Random random;

  public MutationOperator(Random random) {
    this.random = random;
  }

  /**
   * Applies mutation to a chromosome based on mutation rates.
   *
   * @param chromosome The chromosome to mutate
   * @return Mutated chromosome
   */
  public PathChromosome mutate(PathChromosome chromosome) {
    double mutationRoll = random.nextDouble();

    if (mutationRoll < PERTURBATION_RATE) {
      return applyPerturbation(chromosome);
    } else if (mutationRoll < PERTURBATION_RATE + SMOOTHING_RATE) {
      return applySmoothing(chromosome);
    } else if (mutationRoll < PERTURBATION_RATE + SMOOTHING_RATE + REGROWTH_RATE) {
      return applyRegrowth(chromosome);
    }

    return chromosome;
  }

  /**
   * Perturbation mutation: Moves a random waypoint to a neighboring cell.
   * Introduces small local changes to explore nearby solutions.
   */
  private PathChromosome applyPerturbation(PathChromosome chromosome) {
    List<Coordinate> path = new ArrayList<>(chromosome.getPath());
    if (path.size() < 2) {
      return chromosome;
    }

    int index = random.nextInt(path.size() - 1) + 1;
    Coordinate current = path.get(index);
    List<Coordinate> neighbors = getWalkableNeighbors(chromosome.getMaze(), current);

    if (!neighbors.isEmpty()) {
      path.set(index, neighbors.get(random.nextInt(neighbors.size())));
    }

    return new PathChromosome(path, chromosome.getMaze()).repair();
  }

  /**
   * Smoothing mutation: Checks line-of-sight between non-adjacent waypoints
   * and short-circuits the path if a direct connection is valid.
   * Reduces path length and cost by removing unnecessary detours.
   */
  private PathChromosome applySmoothing(PathChromosome chromosome) {
    List<Coordinate> path = new ArrayList<>(chromosome.getPath());
    if (path.size() < 3) {
      return chromosome;
    }

    int i = random.nextInt(path.size() - 2);
    int k = random.nextInt(path.size() - i - 1) + 2;

    if (i + k < path.size() && hasLineOfSight(chromosome.getMaze(), path.get(i), path.get(i + k))) {
      List<Coordinate> smoothed = new ArrayList<>(path.subList(0, i + 1));
      smoothed.add(path.get(i + k));
      smoothed.addAll(path.subList(i + k + 1, path.size()));
      return new PathChromosome(smoothed, chromosome.getMaze()).repair();
    }

    return chromosome;
  }

  /**
   * Regrowth mutation: Deletes a segment and uses A* to bridge the gap.
   * Allows the path to explore completely new routes by replacing segments.
   */
  private PathChromosome applyRegrowth(PathChromosome chromosome) {
    List<Coordinate> path = new ArrayList<>(chromosome.getPath());
    if (path.size() < 3) {
      return chromosome;
    }

    int startIndex = random.nextInt(path.size() - 1);
    int endIndex = random.nextInt(path.size() - startIndex - 1) + startIndex + 1;

    Coordinate from = path.get(startIndex);
    Coordinate to = path.get(endIndex);

    int maxDepth = Math.min(50, chromosome.getMaze().getWidth() * chromosome.getMaze().getHeight() / 4);
    List<Coordinate> bridge = AStarHelper.findPath(chromosome.getMaze(), from, to, maxDepth);

    if (!bridge.isEmpty()) {
      List<Coordinate> regrown = new ArrayList<>(path.subList(0, startIndex + 1));
      regrown.addAll(bridge.subList(1, bridge.size()));
      regrown.addAll(path.subList(endIndex + 1, path.size()));
      return new PathChromosome(regrown, chromosome.getMaze()).repair();
    }

    return chromosome;
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

  private boolean hasLineOfSight(Maze maze, Coordinate from, Coordinate to) {
    int rowDiff = to.row() - from.row();
    int colDiff = to.column() - from.column();

    if (rowDiff == 0 && colDiff == 0) {
      return true;
    }

    if (rowDiff != 0 && colDiff != 0) {
      return false;
    }

    int steps = Math.max(Math.abs(rowDiff), Math.abs(colDiff));
    int rowStep = rowDiff != 0 ? (rowDiff > 0 ? 1 : -1) : 0;
    int colStep = colDiff != 0 ? (colDiff > 0 ? 1 : -1) : 0;

    for (int i = 1; i < steps; i++) {
      int checkRow = from.row() + i * rowStep;
      int checkCol = from.column() + i * colStep;
      Coordinate check = new Coordinate(checkRow, checkCol);

      if (!maze.getCell(check).isWalkable()) {
        return false;
      }
    }

    return true;
  }
}
