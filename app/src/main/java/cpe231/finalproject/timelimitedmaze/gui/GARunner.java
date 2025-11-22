package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GARunner {
  private final Maze maze;
  private final Random random;
  private List<PathIndividual> population;
  private int generation;
  private PathIndividual bestIndividual;

  public GARunner(Maze maze) {
    this.maze = maze;
    this.random = new Random();
    this.generation = 0;
    this.bestIndividual = null;
    initializePopulation();
  }

  private void initializePopulation() {
    int maxPathLength = Math.max(maze.getWidth(), maze.getHeight()) * 3;
    population = new ArrayList<>();

    for (int i = 0; i < GUIConstants.POPULATION_SIZE; i++) {
      List<Coordinate> path = createRandomWalk(maze, maze.getStart(), maxPathLength);
      population.add(new PathIndividual(path, 0.0));
    }

    evaluateFitness();
    bestIndividual = getBest();
  }

  private List<Coordinate> createRandomWalk(Maze maze, Coordinate start, int maxLength) {
    List<Coordinate> path = new ArrayList<>();
    path.add(start);
    Coordinate current = start;
    Set<Coordinate> visited = new HashSet<>();
    visited.add(current);

    for (int step = 0; step < maxLength; step++) {
      if (current.equals(maze.getGoal())) {
        break;
      }

      List<Coordinate> neighbors = getWalkableNeighbors(maze, current);
      neighbors.removeAll(visited);

      if (neighbors.isEmpty()) {
        neighbors = getWalkableNeighbors(maze, current);
        if (neighbors.isEmpty()) {
          break;
        }
      }

      current = neighbors.get(random.nextInt(neighbors.size()));
      path.add(current);
      visited.add(current);
    }

    return path;
  }

  private List<Coordinate> getWalkableNeighbors(Maze maze, Coordinate coord) {
    List<Coordinate> neighbors = new ArrayList<>();
    int[][] deltas = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

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

  private void evaluateFitness() {
    Coordinate goal = maze.getGoal();

    for (PathIndividual individual : population) {
      if (individual.path.isEmpty()) {
        individual.fitness = Double.NEGATIVE_INFINITY;
        continue;
      }

      Coordinate end = individual.path.getLast();
      int distance = manhattanDistance(end, goal);

      if (distance == 0) {
        individual.fitness = 1000000.0 - individual.path.size();
      } else {
        individual.fitness = 1000000.0 / (1.0 + distance);
      }
    }
  }

  private PathIndividual getBest() {
    return Collections.max(population, (a, b) -> Double.compare(a.fitness, b.fitness));
  }

  private List<PathIndividual> getElite(int count) {
    List<PathIndividual> sorted = new ArrayList<>(population);
    sorted.sort((a, b) -> Double.compare(b.fitness, a.fitness));
    return sorted.subList(0, Math.min(count, sorted.size()));
  }

  private PathIndividual selectParent() {
    int tournamentSize = 3;
    PathIndividual best = null;
    double bestFitness = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < tournamentSize; i++) {
      PathIndividual candidate = population.get(random.nextInt(population.size()));
      if (candidate.fitness > bestFitness) {
        best = candidate;
        bestFitness = candidate.fitness;
      }
    }

    return best;
  }

  private List<Coordinate> crossover(List<Coordinate> path1, List<Coordinate> path2) {
    if (path1.isEmpty() || path2.isEmpty()) {
      return path1.isEmpty() ? new ArrayList<>(path2) : new ArrayList<>(path1);
    }

    Set<Coordinate> path1Set = new HashSet<>(path1);
    List<Coordinate> intersection = new ArrayList<>();

    for (Coordinate coord : path2) {
      if (path1Set.contains(coord)) {
        intersection.add(coord);
      }
    }

    if (intersection.isEmpty()) {
      return new ArrayList<>(path1);
    }

    Coordinate crossoverPoint = intersection.get(random.nextInt(intersection.size()));
    int index1 = path1.indexOf(crossoverPoint);
    int index2 = path2.indexOf(crossoverPoint);

    if (index1 == -1 || index2 == -1) {
      return new ArrayList<>(path1);
    }

    List<Coordinate> child = new ArrayList<>();
    child.addAll(path1.subList(0, index1 + 1));
    child.addAll(path2.subList(index2 + 1, path2.size()));

    return child;
  }

  private List<Coordinate> mutate(List<Coordinate> path, int maxLength) {
    if (path.isEmpty()) {
      return path;
    }

    List<Coordinate> mutated = new ArrayList<>(path);

    int mutationType = random.nextInt(3);

    if (mutationType == 0 && mutated.size() > 1) {
      int removeIndex = random.nextInt(mutated.size() - 1) + 1;
      mutated.remove(removeIndex);
    } else if (mutationType == 1 && mutated.size() < maxLength) {
      int insertIndex = random.nextInt(mutated.size());
      Coordinate current = mutated.get(insertIndex);
      List<Coordinate> neighbors = getWalkableNeighbors(maze, current);
      if (!neighbors.isEmpty()) {
        Coordinate newCoord = neighbors.get(random.nextInt(neighbors.size()));
        if (!mutated.contains(newCoord) || insertIndex == mutated.size() - 1) {
          mutated.add(insertIndex + 1, newCoord);
        }
      }
    } else if (mutationType == 2 && mutated.size() > 1) {
      int changeIndex = random.nextInt(mutated.size() - 1) + 1;
      Coordinate prev = mutated.get(changeIndex - 1);
      List<Coordinate> neighbors = getWalkableNeighbors(maze, prev);
      if (!neighbors.isEmpty()) {
        mutated.set(changeIndex, neighbors.get(random.nextInt(neighbors.size())));
      }
    }

    return mutated;
  }

  public GenerationResult getCurrentState() {
    List<Coordinate> bestPath = bestIndividual != null ? new ArrayList<>(bestIndividual.path) : new ArrayList<>();
    boolean goalReached = bestIndividual != null && reachesGoal(bestPath);
    List<List<Coordinate>> allPaths = new ArrayList<>();
    for (PathIndividual individual : population) {
      allPaths.add(new ArrayList<>(individual.path));
    }
    return new GenerationResult(generation, bestPath, bestIndividual != null ? bestIndividual.fitness : Double.NEGATIVE_INFINITY, goalReached, allPaths);
  }

  public GenerationResult nextGeneration() {
    if (generation >= GUIConstants.MAX_GENERATIONS) {
      return getCurrentState();
    }

    int maxPathLength = Math.max(maze.getWidth(), maze.getHeight()) * 3;
    int eliteCount = (int) (GUIConstants.POPULATION_SIZE * GUIConstants.ELITE_PERCENTAGE);
    List<PathIndividual> elite = getElite(eliteCount);
    List<PathIndividual> newPopulation = new ArrayList<>(elite);

    while (newPopulation.size() < GUIConstants.POPULATION_SIZE) {
      PathIndividual parent1 = selectParent();
      PathIndividual parent2 = selectParent();

      List<Coordinate> childPath = crossover(parent1.path, parent2.path);
      if (random.nextDouble() < 0.1) {
        childPath = mutate(childPath, maxPathLength);
      }

      newPopulation.add(new PathIndividual(childPath, 0.0));
    }

    population = newPopulation;
    evaluateFitness();

    PathIndividual currentBest = getBest();
    if (bestIndividual == null || currentBest.fitness > bestIndividual.fitness) {
      bestIndividual = currentBest;
    }

    generation++;

    List<Coordinate> bestPath = bestIndividual != null ? new ArrayList<>(bestIndividual.path) : new ArrayList<>();
    boolean goalReached = bestIndividual != null && reachesGoal(bestPath);
    List<List<Coordinate>> allPaths = new ArrayList<>();
    for (PathIndividual individual : population) {
      allPaths.add(new ArrayList<>(individual.path));
    }

    return new GenerationResult(generation, bestPath, bestIndividual != null ? bestIndividual.fitness : Double.NEGATIVE_INFINITY, goalReached, allPaths);
  }

  private boolean reachesGoal(List<Coordinate> path) {
    if (path.isEmpty()) {
      return false;
    }
    return path.getLast().equals(maze.getGoal());
  }

  private int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column());
  }

  private static class PathIndividual {
    List<Coordinate> path;
    double fitness;

    PathIndividual(List<Coordinate> path, double fitness) {
      this.path = path;
      this.fitness = fitness;
    }
  }

  public record GenerationResult(int generation, List<Coordinate> bestPath, double bestFitness, boolean goalReached,
      List<List<Coordinate>> allPaths) {
  }
}
