package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Simple Genetic Algorithm solver for mazes.
 *
 * This solver uses a simplified genetic algorithm to find paths through mazes.
 * It maintains a population of paths, evaluates fitness based on distance to
 * goal,
 * and evolves the population through selection, crossover, and mutation.
 */
public final class GeneticAlgorithmSolver extends MazeSolver {

  private static final int DEFAULT_POPULATION_SIZE = 50;
  private static final int DEFAULT_MAX_GENERATIONS = 100;
  private static final double ELITE_PERCENTAGE = 0.2;
  private static final double MUTATION_RATE = 0.1;

  private final int populationSize;
  private final int maxGenerations;
  private final Random random;

  public GeneticAlgorithmSolver() {
    this(DEFAULT_POPULATION_SIZE, DEFAULT_MAX_GENERATIONS, new Random());
  }

  public GeneticAlgorithmSolver(int populationSize, int maxGenerations, Random random) {
    if (populationSize < 10) {
      throw new IllegalArgumentException("Population size must be at least 10");
    }
    if (maxGenerations < 1) {
      throw new IllegalArgumentException("Max generations must be at least 1");
    }
    this.populationSize = populationSize;
    this.maxGenerations = maxGenerations;
    this.random = random != null ? random : new Random();
  }

  @Override
  public String getAlgorithmName() {
    return "Genetic Algorithm";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int maxPathLength = Math.max(maze.getWidth(), maze.getHeight()) * 3;
    List<PathIndividual> population = initializePopulation(maze, maxPathLength);

    for (int generation = 0; generation < maxGenerations; generation++) {
      evaluateFitness(population, maze);

      PathIndividual best = getBest(population);
      if (reachesGoal(best.path, maze)) {
        return repairPath(best.path, maze);
      }

      int eliteCount = (int) (populationSize * ELITE_PERCENTAGE);
      List<PathIndividual> elite = getElite(population, eliteCount);
      List<PathIndividual> newPopulation = new ArrayList<>(elite);

      while (newPopulation.size() < populationSize) {
        PathIndividual parent1 = selectParent(population);
        PathIndividual parent2 = selectParent(population);

        List<Coordinate> childPath = crossover(parent1.path, parent2.path, maze);
        if (random.nextDouble() < MUTATION_RATE) {
          childPath = mutate(childPath, maze, maxPathLength);
        }
        childPath = repairPath(childPath, maze);

        newPopulation.add(new PathIndividual(childPath, 0.0));
      }

      population = newPopulation;
    }

    evaluateFitness(population, maze);
    PathIndividual best = getBest(population);
    List<Coordinate> solution = repairPath(best.path, maze);

    if (!reachesGoal(solution, maze)) {
      throw new MazeSolvingException("Genetic algorithm failed to find a solution");
    }

    return solution;
  }

  private List<PathIndividual> initializePopulation(Maze maze, int maxLength) {
    List<PathIndividual> population = new ArrayList<>();
    Coordinate start = maze.getStart();

    for (int i = 0; i < populationSize; i++) {
      List<Coordinate> path = createRandomWalk(maze, start, maxLength);
      population.add(new PathIndividual(path, 0.0));
    }

    return population;
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

      List<Coordinate> neighbors = walkableNeighbors(maze, current);
      neighbors.removeAll(visited);

      if (neighbors.isEmpty()) {
        neighbors = walkableNeighbors(maze, current);
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

  private void evaluateFitness(List<PathIndividual> population, Maze maze) {
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

  private PathIndividual getBest(List<PathIndividual> population) {
    return Collections.max(population, (a, b) -> Double.compare(a.fitness, b.fitness));
  }

  private List<PathIndividual> getElite(List<PathIndividual> population, int count) {
    List<PathIndividual> sorted = new ArrayList<>(population);
    sorted.sort((a, b) -> Double.compare(b.fitness, a.fitness));
    return sorted.subList(0, Math.min(count, sorted.size()));
  }

  private PathIndividual selectParent(List<PathIndividual> population) {
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

  private List<Coordinate> crossover(List<Coordinate> path1, List<Coordinate> path2, Maze maze) {
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

  private List<Coordinate> mutate(List<Coordinate> path, Maze maze, int maxLength) {
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
      List<Coordinate> neighbors = walkableNeighbors(maze, current);
      if (!neighbors.isEmpty()) {
        Coordinate newCoord = neighbors.get(random.nextInt(neighbors.size()));
        if (!mutated.contains(newCoord) || insertIndex == mutated.size() - 1) {
          mutated.add(insertIndex + 1, newCoord);
        }
      }
    } else if (mutationType == 2 && mutated.size() > 1) {
      int changeIndex = random.nextInt(mutated.size() - 1) + 1;
      Coordinate prev = mutated.get(changeIndex - 1);
      List<Coordinate> neighbors = walkableNeighbors(maze, prev);
      if (!neighbors.isEmpty()) {
        mutated.set(changeIndex, neighbors.get(random.nextInt(neighbors.size())));
      }
    }

    return mutated;
  }

  private List<Coordinate> repairPath(List<Coordinate> path, Maze maze) {
    if (path.isEmpty()) {
      return new ArrayList<>();
    }

    List<Coordinate> repaired = new ArrayList<>();
    repaired.add(maze.getStart());

    for (int i = 1; i < path.size(); i++) {
      Coordinate current = repaired.getLast();
      Coordinate target = path.get(i);

      if (isAdjacent(current, target) && isWalkable(maze, target)) {
        repaired.add(target);
      } else {
        List<Coordinate> pathToTarget = findPathToTarget(maze, current, target, repaired);
        if (pathToTarget != null) {
          repaired.addAll(pathToTarget);
        }
      }

      if (repaired.getLast().equals(maze.getGoal())) {
        break;
      }
    }

    return repaired;
  }

  private List<Coordinate> findPathToTarget(Maze maze, Coordinate from, Coordinate to,
      List<Coordinate> visited) {
    Set<Coordinate> visitedSet = new HashSet<>(visited);
    List<List<Coordinate>> queue = new ArrayList<>();
    queue.add(List.of(from));

    int maxSteps = 20;
    for (int step = 0; step < maxSteps && !queue.isEmpty(); step++) {
      List<List<Coordinate>> nextQueue = new ArrayList<>();

      for (List<Coordinate> currentPath : queue) {
        Coordinate current = currentPath.getLast();

        if (current.equals(to)) {
          return currentPath.subList(1, currentPath.size());
        }

        for (Coordinate neighbor : walkableNeighbors(maze, current)) {
          if (!visitedSet.contains(neighbor) && !currentPath.contains(neighbor)) {
            List<Coordinate> newPath = new ArrayList<>(currentPath);
            newPath.add(neighbor);
            nextQueue.add(newPath);
          }
        }
      }

      queue = nextQueue;
    }

    return null;
  }

  private boolean reachesGoal(List<Coordinate> path, Maze maze) {
    if (path.isEmpty()) {
      return false;
    }
    return path.getLast().equals(maze.getGoal());
  }

  private int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column());
  }

  private boolean isAdjacent(Coordinate a, Coordinate b) {
    int rowDiff = Math.abs(a.row() - b.row());
    int colDiff = Math.abs(a.column() - b.column());
    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
  }

  private static class PathIndividual {
    List<Coordinate> path;
    double fitness;

    PathIndividual(List<Coordinate> path, double fitness) {
      this.path = path;
      this.fitness = fitness;
    }
  }
}
