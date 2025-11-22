package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Intersection crossover operator for genetic algorithm.
 *
 * Crossover (recombination) combines genetic material from two parents to
 * create
 * offspring. This operator finds common coordinates (intersection points)
 * between
 * two paths and swaps the tails, creating new path combinations that inherit
 * beneficial segments from both parents.
 *
 * If no intersection exists, falls back to arithmetic crossover (averaging
 * waypoint
 * positions) followed by path repair to ensure validity.
 */
public final class IntersectionCrossover {

  public IntersectionCrossover(Random random) {
  }

  /**
   * Performs crossover between two parent chromosomes to create offspring.
   *
   * @param parent1 First parent chromosome
   * @param parent2 Second parent chromosome
   * @return List containing two offspring chromosomes
   */
  public List<PathChromosome> crossover(PathChromosome parent1, PathChromosome parent2) {
    List<Coordinate> path1 = parent1.getPath();
    List<Coordinate> path2 = parent2.getPath();

    Integer intersectionIndex1 = findIntersection(path1, path2);
    Integer intersectionIndex2 = intersectionIndex1 != null ? findIndexInPath(path2, path1.get(intersectionIndex1))
        : null;

    if (intersectionIndex1 != null && intersectionIndex2 != null) {
      return performTailSwap(path1, path2, intersectionIndex1, intersectionIndex2,
          parent1.getMaze());
    } else {
      return performArithmeticCrossover(parent1, parent2);
    }
  }

  private Integer findIntersection(List<Coordinate> path1, List<Coordinate> path2) {
    for (int i = 0; i < path1.size(); i++) {
      if (path2.contains(path1.get(i))) {
        return i;
      }
    }
    return null;
  }

  private Integer findIndexInPath(List<Coordinate> path, Coordinate target) {
    for (int i = 0; i < path.size(); i++) {
      if (path.get(i).equals(target)) {
        return i;
      }
    }
    return null;
  }

  private List<PathChromosome> performTailSwap(List<Coordinate> path1,
      List<Coordinate> path2,
      int index1, int index2,
      cpe231.finalproject.timelimitedmaze.utils.Maze maze) {
    List<Coordinate> offspring1 = new ArrayList<>(path1.subList(0, index1 + 1));
    offspring1.addAll(path2.subList(index2 + 1, path2.size()));

    List<Coordinate> offspring2 = new ArrayList<>(path2.subList(0, index2 + 1));
    offspring2.addAll(path1.subList(index1 + 1, path1.size()));

    PathChromosome child1 = new PathChromosome(offspring1, maze).repair();
    PathChromosome child2 = new PathChromosome(offspring2, maze).repair();

    return List.of(child1, child2);
  }

  private List<PathChromosome> performArithmeticCrossover(PathChromosome parent1,
      PathChromosome parent2) {
    List<Coordinate> path1 = parent1.getPath();
    List<Coordinate> path2 = parent2.getPath();
    cpe231.finalproject.timelimitedmaze.utils.Maze maze = parent1.getMaze();

    int maxLength = Math.max(path1.size(), path2.size());
    List<Coordinate> offspring1 = new ArrayList<>();
    List<Coordinate> offspring2 = new ArrayList<>();

    for (int i = 0; i < maxLength; i++) {
      Coordinate coord1 = i < path1.size() ? path1.get(i) : path1.getLast();
      Coordinate coord2 = i < path2.size() ? path2.get(i) : path2.getLast();

      int avgRow = (coord1.row() + coord2.row()) / 2;
      int avgCol = (coord1.column() + coord2.column()) / 2;

      Coordinate avgCoord1 = new Coordinate(avgRow, avgCol);
      Coordinate avgCoord2 = new Coordinate(avgRow, avgCol);

      if (i < path1.size()) {
        offspring1.add(avgCoord1);
      }
      if (i < path2.size()) {
        offspring2.add(avgCoord2);
      }
    }

    PathChromosome child1 = new PathChromosome(offspring1, maze).repair();
    PathChromosome child2 = new PathChromosome(offspring2, maze).repair();

    return List.of(child1, child2);
  }
}
