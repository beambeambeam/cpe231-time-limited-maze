package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * Improved Genetic Algorithm solver for mazes with weighted tiles.
 *
 * Uses adaptive parameters, cost-aware fitness evaluation, heuristic-guided
 * initialization, and local search optimization (memetic algorithm approach).
 */
public final class GeneticAlgorithmSolver extends MazeSolver {

  private static final int BASE_POPULATION_SIZE = 100;
  private static final int BASE_MAX_GENERATIONS = 200;
  private static final double ELITE_PERCENTAGE = 0.15;
  private static final double BASE_MUTATION_RATE = 0.15;
  private static final double HEURISTIC_PROBABILITY = 0.7;
  private static final int TOURNAMENT_SIZE = 5;

  private final int populationSize;
  private final int maxGenerations;
  private final Random random;

  public GeneticAlgorithmSolver() {
    this(BASE_POPULATION_SIZE, BASE_MAX_GENERATIONS, new Random());
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
    int mazeSize = maze.getWidth() * maze.getHeight();
    int adaptivePopSize = Math.max(populationSize, (int) Math.sqrt(mazeSize) * 2);
    int adaptiveMaxGen = Math.max(maxGenerations, (int) Math.sqrt(mazeSize) * 3);
    int maxPathLength = (maze.getWidth() + maze.getHeight()) * 2;
    log("GA start grid " + maze.getHeight() + "x" + maze.getWidth()
        + " pop " + adaptivePopSize + " generations " + adaptiveMaxGen
        + " maxPathLength " + maxPathLength);

    List<PathIndividual> population = initializePopulation(maze, maxPathLength, adaptivePopSize);
    PathIndividual bestEver = null;

    for (int generation = 0; generation < adaptiveMaxGen; generation++) {
      evaluateFitness(population, maze);

      PathIndividual currentBest = getBest(population);
      if (bestEver == null || currentBest.fitness > bestEver.fitness) {
        bestEver = new PathIndividual(new ArrayList<>(currentBest.path), currentBest.fitness);
      }

      if (reachesGoal(currentBest.path, maze)) {
        List<Coordinate> optimized = localSearchOptimize(currentBest.path, maze);
        log("GA found goal at generation " + generation + " path length " + optimized.size());
        return optimized;
      }

      double diversity = calculateDiversity(population);
      double mutationRate = adaptiveMutationRate(diversity, generation, adaptiveMaxGen);

      if (generation % 10 == 0) {
        int bestPathLength = currentBest.path.size();
        int distanceToGoal = manhattanDistance(currentBest.path.getLast(), maze.getGoal());
        int bestPathCost = calculatePathCost(maze, currentBest.path);
        log("GA generation " + generation + " best path length " + bestPathLength
            + " distance to goal " + distanceToGoal + " cost " + bestPathCost
            + " diversity " + String.format("%.3f", diversity) + " mutation " + String.format("%.3f", mutationRate));
      }

      int eliteCount = Math.max(2, (int) (adaptivePopSize * ELITE_PERCENTAGE));
      List<PathIndividual> elite = getElite(population, eliteCount);

      for (PathIndividual eliteIndividual : elite) {
        if (reachesGoal(eliteIndividual.path, maze)) {
          eliteIndividual.path = localSearchOptimize(eliteIndividual.path, maze);
          evaluateSingleFitness(eliteIndividual, maze);
        }
      }

      List<PathIndividual> newPopulation = new ArrayList<>(elite);

      while (newPopulation.size() < adaptivePopSize) {
        PathIndividual parent1 = selectParent(population);
        PathIndividual parent2 = selectParent(population);

        List<Coordinate> childPath = crossover(parent1.path, parent2.path, maze);

        if (random.nextDouble() < mutationRate) {
          childPath = mutate(childPath, maze, maxPathLength);
        }

        childPath = repairPath(childPath, maze, maxPathLength);

        if (!childPath.isEmpty()) {
          newPopulation.add(new PathIndividual(childPath, 0.0));
        }
      }

      population = newPopulation;
    }

    evaluateFitness(population, maze);
    PathIndividual best = getBest(population);

    if (bestEver != null && bestEver.fitness > best.fitness) {
      best = bestEver;
    }

    List<Coordinate> solution = repairPath(best.path, maze, maxPathLength);

    if (!reachesGoal(solution, maze)) {
      solution = findPathAStar(maze, maze.getStart(), maze.getGoal());
      if (solution == null || !reachesGoal(solution, maze)) {
        log("GA failed to reach goal; fallback A* also failed");
        throw new MazeSolvingException("Genetic algorithm failed to find a solution");
      }
      log("GA used fallback A* path length " + solution.size());
    }

    List<Coordinate> optimized = localSearchOptimize(solution, maze);
    log("GA final optimized path length " + optimized.size());
    return optimized;
  }

  private List<PathIndividual> initializePopulation(Maze maze, int maxLength, int popSize) {
    List<PathIndividual> population = new ArrayList<>();
    Coordinate start = maze.getStart();

    List<Coordinate> aStarPath = findPathAStar(maze, start, maze.getGoal());
    if (aStarPath != null && !aStarPath.isEmpty()) {
      population.add(new PathIndividual(aStarPath, 0.0));
    }

    for (int i = population.size(); i < popSize; i++) {
      List<Coordinate> path;
      if (random.nextDouble() < HEURISTIC_PROBABILITY) {
        path = createHeuristicWalk(maze, start, maxLength);
      } else {
        path = createRandomWalk(maze, start, maxLength);
      }
      population.add(new PathIndividual(path, 0.0));
    }

    return population;
  }

  private List<Coordinate> createHeuristicWalk(Maze maze, Coordinate start, int maxLength) {
    List<Coordinate> path = new ArrayList<>();
    path.add(start);
    Coordinate current = start;
    Set<Coordinate> visited = new HashSet<>();
    visited.add(current);
    Coordinate goal = maze.getGoal();

    for (int step = 0; step < maxLength; step++) {
      if (current.equals(goal)) {
        break;
      }

      List<Coordinate> neighbors = walkableNeighbors(maze, current);
      List<Coordinate> unvisited = new ArrayList<>();
      for (Coordinate n : neighbors) {
        if (!visited.contains(n)) {
          unvisited.add(n);
        }
      }

      if (unvisited.isEmpty()) {
        if (neighbors.isEmpty()) {
          break;
        }
        current = neighbors.get(random.nextInt(neighbors.size()));
      } else {
        if (random.nextDouble() < 0.3) {
          current = unvisited.get(random.nextInt(unvisited.size()));
        } else {
          current = selectBestNeighbor(maze, unvisited, goal);
        }
      }

      path.add(current);
      visited.add(current);
    }

    return path;
  }

  private Coordinate selectBestNeighbor(Maze maze, List<Coordinate> neighbors, Coordinate goal) {
    Coordinate best = neighbors.get(0);
    double bestScore = Double.MAX_VALUE;

    for (Coordinate neighbor : neighbors) {
      int distance = manhattanDistance(neighbor, goal);
      int cost = stepCost(maze, neighbor);
      double score = distance * 2.0 + cost;

      if (score < bestScore) {
        bestScore = score;
        best = neighbor;
      }
    }

    return best;
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
      List<Coordinate> unvisited = new ArrayList<>();
      for (Coordinate n : neighbors) {
        if (!visited.contains(n)) {
          unvisited.add(n);
        }
      }

      if (unvisited.isEmpty()) {
        if (neighbors.isEmpty()) {
          break;
        }
        current = neighbors.get(random.nextInt(neighbors.size()));
      } else {
        current = unvisited.get(random.nextInt(unvisited.size()));
      }

      path.add(current);
      visited.add(current);
    }

    return path;
  }

  private void evaluateFitness(List<PathIndividual> population, Maze maze) {
    for (PathIndividual individual : population) {
      evaluateSingleFitness(individual, maze);
    }
  }

  private void evaluateSingleFitness(PathIndividual individual, Maze maze) {
    if (individual.path.isEmpty()) {
      individual.fitness = Double.NEGATIVE_INFINITY;
      return;
    }

    Coordinate goal = maze.getGoal();
    Coordinate end = individual.path.getLast();
    int distance = manhattanDistance(end, goal);
    int pathCost = calculatePathCost(maze, individual.path);

    if (distance == 0) {
      individual.fitness = 1_000_000.0 - pathCost;
    } else {
      individual.fitness = 1_000_000.0 / (1.0 + distance * 10.0 + pathCost * 0.1);
    }
  }

  private double calculateDiversity(List<PathIndividual> population) {
    if (population.size() < 2) {
      return 1.0;
    }

    Set<Coordinate> uniqueEndpoints = new HashSet<>();
    Set<Integer> uniqueLengths = new HashSet<>();

    for (PathIndividual individual : population) {
      if (!individual.path.isEmpty()) {
        uniqueEndpoints.add(individual.path.getLast());
        uniqueLengths.add(individual.path.size());
      }
    }

    double endpointDiversity = (double) uniqueEndpoints.size() / population.size();
    double lengthDiversity = (double) uniqueLengths.size() / population.size();

    return (endpointDiversity + lengthDiversity) / 2.0;
  }

  private double adaptiveMutationRate(double diversity, int generation, int maxGen) {
    double diversityFactor = 1.0 - diversity;
    double progressFactor = (double) generation / maxGen;
    return Math.min(0.5, BASE_MUTATION_RATE + diversityFactor * 0.25 + progressFactor * 0.1);
  }

  private PathIndividual getBest(List<PathIndividual> population) {
    return Collections.max(population, Comparator.comparingDouble(a -> a.fitness));
  }

  private List<PathIndividual> getElite(List<PathIndividual> population, int count) {
    List<PathIndividual> sorted = new ArrayList<>(population);
    sorted.sort((a, b) -> Double.compare(b.fitness, a.fitness));

    List<PathIndividual> elite = new ArrayList<>();
    for (int i = 0; i < Math.min(count, sorted.size()); i++) {
      PathIndividual original = sorted.get(i);
      elite.add(new PathIndividual(new ArrayList<>(original.path), original.fitness));
    }
    return elite;
  }

  private PathIndividual selectParent(List<PathIndividual> population) {
    PathIndividual best = null;
    double bestFitness = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < TOURNAMENT_SIZE; i++) {
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

    if (!intersection.isEmpty()) {
      Coordinate crossoverPoint = intersection.get(random.nextInt(intersection.size()));
      int index1 = path1.indexOf(crossoverPoint);
      int index2 = path2.indexOf(crossoverPoint);

      if (index1 != -1 && index2 != -1) {
        List<Coordinate> child = new ArrayList<>();
        child.addAll(path1.subList(0, index1 + 1));
        if (index2 + 1 < path2.size()) {
          child.addAll(path2.subList(index2 + 1, path2.size()));
        }
        return child;
      }
    }

    int splitPoint1 = Math.max(1, path1.size() / 2);
    List<Coordinate> child = new ArrayList<>(path1.subList(0, splitPoint1));

    Coordinate bridgeStart = child.getLast();
    int bridgeTarget = random.nextInt(Math.max(1, path2.size() - 1)) + 1;
    Coordinate bridgeEnd = path2.get(Math.min(bridgeTarget, path2.size() - 1));

    List<Coordinate> bridge = findPathAStar(maze, bridgeStart, bridgeEnd);
    if (bridge != null && bridge.size() > 1) {
      child.addAll(bridge.subList(1, bridge.size()));
      if (bridgeTarget + 1 < path2.size()) {
        child.addAll(path2.subList(bridgeTarget + 1, path2.size()));
      }
    } else {
      return random.nextBoolean() ? new ArrayList<>(path1) : new ArrayList<>(path2);
    }

    return child;
  }

  private List<Coordinate> mutate(List<Coordinate> path, Maze maze, int maxLength) {
    if (path.isEmpty()) {
      return path;
    }

    List<Coordinate> mutated = new ArrayList<>(path);
    int mutationType = random.nextInt(5);

    switch (mutationType) {
      case 0 -> {
        if (mutated.size() > 2) {
          int removeIndex = random.nextInt(mutated.size() - 2) + 1;
          mutated.remove(removeIndex);
        }
      }
      case 1 -> {
        if (mutated.size() < maxLength) {
          int insertIndex = random.nextInt(mutated.size());
          Coordinate current = mutated.get(insertIndex);
          List<Coordinate> neighbors = walkableNeighbors(maze, current);
          if (!neighbors.isEmpty()) {
            Set<Coordinate> pathSet = new HashSet<>(mutated);
            List<Coordinate> validNeighbors = new ArrayList<>();
            for (Coordinate n : neighbors) {
              if (!pathSet.contains(n)) {
                validNeighbors.add(n);
              }
            }
            if (!validNeighbors.isEmpty()) {
              mutated.add(insertIndex + 1, validNeighbors.get(random.nextInt(validNeighbors.size())));
            }
          }
        }
      }
      case 2 -> {
        if (mutated.size() > 2) {
          int changeIndex = random.nextInt(mutated.size() - 2) + 1;
          Coordinate prev = mutated.get(changeIndex - 1);
          List<Coordinate> neighbors = walkableNeighbors(maze, prev);
          if (!neighbors.isEmpty()) {
            Coordinate next = changeIndex + 1 < mutated.size() ? mutated.get(changeIndex + 1) : null;
            List<Coordinate> validNeighbors = new ArrayList<>();
            for (Coordinate n : neighbors) {
              if (next == null || isAdjacent(n, next) || n.equals(next)) {
                validNeighbors.add(n);
              }
            }
            if (!validNeighbors.isEmpty()) {
              mutated.set(changeIndex, validNeighbors.get(random.nextInt(validNeighbors.size())));
            } else {
              mutated.set(changeIndex, neighbors.get(random.nextInt(neighbors.size())));
            }
          }
        }
      }
      case 3 -> {
        if (mutated.size() > 3) {
          int start = random.nextInt(mutated.size() - 2) + 1;
          int end = Math.min(start + random.nextInt(3) + 1, mutated.size() - 1);
          if (end > start) {
            List<Coordinate> shortcut = findPathAStar(maze, mutated.get(start), mutated.get(end));
            if (shortcut != null && shortcut.size() < end - start + 1) {
              List<Coordinate> newPath = new ArrayList<>(mutated.subList(0, start));
              newPath.addAll(shortcut);
              if (end + 1 < mutated.size()) {
                newPath.addAll(mutated.subList(end + 1, mutated.size()));
              }
              mutated = newPath;
            }
          }
        }
      }
      case 4 -> {
        if (!reachesGoal(mutated, maze) && mutated.size() > 1) {
          Coordinate lastPos = mutated.getLast();
          List<Coordinate> extension = findPathAStar(maze, lastPos, maze.getGoal());
          if (extension != null && extension.size() > 1) {
            int addCount = Math.min(extension.size() - 1, maxLength - mutated.size());
            if (addCount > 0) {
              mutated.addAll(extension.subList(1, Math.min(addCount + 1, extension.size())));
            }
          }
        }
      }
      default -> {
      }
    }

    return mutated;
  }

  private List<Coordinate> repairPath(List<Coordinate> path, Maze maze, int maxPathLength) {
    if (path.isEmpty()) {
      return createHeuristicWalk(maze, maze.getStart(), maxPathLength);
    }

    List<Coordinate> repaired = new ArrayList<>();
    Set<Coordinate> visited = new HashSet<>();
    repaired.add(maze.getStart());
    visited.add(maze.getStart());

    for (int i = 1; i < path.size() && repaired.size() < maxPathLength; i++) {
      Coordinate current = repaired.getLast();
      Coordinate target = path.get(i);

      if (current.equals(maze.getGoal())) {
        break;
      }

      if (isAdjacent(current, target) && isWalkable(maze, target)) {
        repaired.add(target);
        visited.add(target);
      } else if (isWalkable(maze, target)) {
        List<Coordinate> bridge = findPathAStar(maze, current, target);
        if (bridge != null && bridge.size() > 1) {
          int addCount = Math.min(bridge.size() - 1, maxPathLength - repaired.size());
          for (int j = 1; j <= addCount; j++) {
            Coordinate step = bridge.get(j);
            if (!visited.contains(step) || step.equals(maze.getGoal())) {
              repaired.add(step);
              visited.add(step);
              if (step.equals(maze.getGoal())) {
                break;
              }
            }
          }
        }
      }

      if (repaired.getLast().equals(maze.getGoal())) {
        break;
      }
    }

    if (!reachesGoal(repaired, maze) && repaired.size() < maxPathLength) {
      List<Coordinate> toGoal = findPathAStar(maze, repaired.getLast(), maze.getGoal());
      if (toGoal != null && toGoal.size() > 1) {
        int addCount = Math.min(toGoal.size() - 1, maxPathLength - repaired.size());
        repaired.addAll(toGoal.subList(1, Math.min(addCount + 1, toGoal.size())));
      }
    }

    return repaired;
  }

  private List<Coordinate> findPathAStar(Maze maze, Coordinate from, Coordinate to) {
    if (from.equals(to)) {
      return List.of(from);
    }

    Map<Coordinate, Integer> gScore = new HashMap<>();
    Map<Coordinate, Coordinate> cameFrom = new HashMap<>();
    Set<Coordinate> closedSet = new HashSet<>();

    PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
        Comparator.comparingDouble(n -> n.fScore));

    gScore.put(from, 0);
    openSet.add(new AStarNode(from, manhattanDistance(from, to)));

    int maxIterations = maze.getWidth() * maze.getHeight() * 2;
    int iterations = 0;

    while (!openSet.isEmpty() && iterations < maxIterations) {
      iterations++;
      AStarNode currentNode = openSet.poll();
      Coordinate current = currentNode.coord;

      if (current.equals(to)) {
        return reconstructPath(cameFrom, current);
      }

      if (closedSet.contains(current)) {
        continue;
      }
      closedSet.add(current);

      for (Coordinate neighbor : walkableNeighbors(maze, current)) {
        if (closedSet.contains(neighbor)) {
          continue;
        }

        int tentativeG = gScore.getOrDefault(current, Integer.MAX_VALUE) + stepCost(maze, neighbor);

        if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
          cameFrom.put(neighbor, current);
          gScore.put(neighbor, tentativeG);
          double fScore = tentativeG + manhattanDistance(neighbor, to);
          openSet.add(new AStarNode(neighbor, fScore));
        }
      }
    }

    return null;
  }

  private List<Coordinate> reconstructPath(Map<Coordinate, Coordinate> cameFrom, Coordinate current) {
    List<Coordinate> path = new ArrayList<>();
    path.add(current);
    while (cameFrom.containsKey(current)) {
      current = cameFrom.get(current);
      path.addFirst(current);
    }
    return path;
  }

  private List<Coordinate> localSearchOptimize(List<Coordinate> path, Maze maze) {
    if (path.size() < 3) {
      return path;
    }

    List<Coordinate> optimized = new ArrayList<>(path);
    int currentCost = calculatePathCost(maze, optimized);
    boolean improved = true;
    int maxPasses = 5;
    int passes = 0;

    while (improved && passes < maxPasses) {
      improved = false;
      passes++;

      for (int i = 0; i < optimized.size() - 2 && optimized.size() > 2; i++) {
        for (int j = i + 2; j < optimized.size(); j++) {
          Coordinate start = optimized.get(i);
          Coordinate end = optimized.get(j);

          List<Coordinate> shortcut = findPathAStar(maze, start, end);
          if (shortcut != null && shortcut.size() < j - i + 1) {
            List<Coordinate> newPath = new ArrayList<>(optimized.subList(0, i));
            newPath.addAll(shortcut);
            if (j + 1 < optimized.size()) {
              newPath.addAll(optimized.subList(j + 1, optimized.size()));
            }

            int newCost = calculatePathCost(maze, newPath);
            if (newCost < currentCost && reachesGoal(newPath, maze)) {
              optimized = newPath;
              currentCost = newCost;
              improved = true;
              break;
            }
          }
        }
        if (improved) {
          break;
        }
      }
    }

    return optimized;
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

  private record AStarNode(Coordinate coord, double fScore) {
  }
}
