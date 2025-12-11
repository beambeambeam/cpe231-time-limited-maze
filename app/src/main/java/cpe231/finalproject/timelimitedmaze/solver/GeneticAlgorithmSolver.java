package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure Genetic Algorithm solver for mazes with weighted tiles.
 *
 * Uses direction-based chromosomes where each gene represents a direction
 * (NORTH, EAST, SOUTH, WEST). Chromosomes are executed to generate paths
 * through the maze. Uses crossover and mutation operations on direction genes.
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
    int maxChromosomeLength = (maze.getWidth() + maze.getHeight()) * 2;
    log("GA start grid " + maze.getHeight() + "x" + maze.getWidth()
        + " pop " + adaptivePopSize + " generations " + adaptiveMaxGen
        + " maxChromosomeLength " + maxChromosomeLength);

    List<PathIndividual> population = initializePopulation(maze, maxChromosomeLength, adaptivePopSize);
    PathIndividual bestEver = null;

    for (int generation = 0; generation < adaptiveMaxGen; generation++) {
      evaluateFitness(population, maze);

      PathIndividual currentBest = getBest(population);
      if (bestEver == null || currentBest.fitness > bestEver.fitness) {
        List<Coordinate> bestPath = currentBest.cachedPath != null
            ? new ArrayList<>(currentBest.cachedPath)
            : executeChromosome(currentBest.chromosome, maze);
        bestEver = new PathIndividual(new ArrayList<>(currentBest.chromosome), currentBest.fitness, bestPath);
      }

      List<Coordinate> bestPath = getCachedOrExecutePath(currentBest, maze);
      if (reachesGoal(bestPath, maze)) {
        log("GA found goal at generation " + generation + " path length " + bestPath.size());
        return bestPath;
      }

      double diversity = calculateDiversity(population);
      double mutationRate = adaptiveMutationRate(diversity, generation, adaptiveMaxGen);

      if (generation % 5 == 0) {
        int bestPathLength = bestPath.size();
        int distanceToGoal = bestPath.isEmpty() ? Integer.MAX_VALUE
            : manhattanDistance(bestPath.getLast(), maze.getGoal());
        int bestPathCost = bestPath.isEmpty() ? Integer.MAX_VALUE
            : calculatePathCost(maze, bestPath);
        log("GA generation " + generation + " best path length " + bestPathLength
            + " distance to goal " + distanceToGoal + " cost " + bestPathCost
            + " diversity " + String.format("%.3f", diversity) + " mutation " + String.format("%.3f", mutationRate));
      }

      int eliteCount = Math.max(2, (int) (adaptivePopSize * ELITE_PERCENTAGE));
      List<PathIndividual> elite = getElite(population, eliteCount);

      List<PathIndividual> newPopulation = new ArrayList<>();
      for (PathIndividual eliteIndividual : elite) {
        newPopulation.add(new PathIndividual(
            new ArrayList<>(eliteIndividual.chromosome),
            eliteIndividual.fitness,
            eliteIndividual.cachedPath != null ? new ArrayList<>(eliteIndividual.cachedPath) : null));
      }

      while (newPopulation.size() < adaptivePopSize) {
        PathIndividual parent1 = selectParent(population);
        PathIndividual parent2 = selectParent(population);

        List<Direction> childChromosome = crossoverChromosomes(parent1.chromosome, parent2.chromosome);

        if (random.nextDouble() < mutationRate) {
          childChromosome = mutateChromosome(childChromosome, maxChromosomeLength);
        }

        if (!childChromosome.isEmpty()) {
          PathIndividual child = new PathIndividual(childChromosome, 0.0);
          newPopulation.add(child);
        }
      }

      population = newPopulation;
    }

    evaluateFitness(population, maze);
    PathIndividual best = getBest(population);

    if (bestEver != null && bestEver.fitness > best.fitness) {
      best = bestEver;
    }

    List<Coordinate> solution = getCachedOrExecutePath(best, maze);
    double finalDiversity = calculateDiversity(population);
    double finalMutationRate = adaptiveMutationRate(finalDiversity, adaptiveMaxGen - 1, adaptiveMaxGen);
    int finalPathLength = solution.size();
    int finalDistanceToGoal = solution.isEmpty() ? Integer.MAX_VALUE
        : manhattanDistance(solution.getLast(), maze.getGoal());
    int finalPathCost = solution.isEmpty() ? Integer.MAX_VALUE
        : calculatePathCost(maze, solution);
    log("GA generation " + (adaptiveMaxGen - 1) + " (final) best path length " + finalPathLength
        + " distance to goal " + finalDistanceToGoal + " cost " + finalPathCost
        + " diversity " + String.format("%.3f", finalDiversity) + " mutation " + String.format("%.3f", finalMutationRate));

    if (!reachesGoal(solution, maze)) {
      log("GA failed to reach goal after " + adaptiveMaxGen + " generations");
      throw new MazeSolvingException("Genetic algorithm failed to find a solution");
    }

    log("GA final path length " + solution.size());
    return solution;
  }

  private List<PathIndividual> initializePopulation(Maze maze, int maxLength, int popSize) {
    List<PathIndividual> population = new ArrayList<>();
    Coordinate start = maze.getStart();

    for (int i = 0; i < popSize; i++) {
      List<Direction> chromosome;
      if (random.nextDouble() < HEURISTIC_PROBABILITY) {
        chromosome = initializeHeuristicChromosome(maze, maxLength);
      } else {
        chromosome = initializeRandomChromosome(maxLength);
      }
      population.add(new PathIndividual(chromosome, 0.0));
    }

    return population;
  }

  private List<Direction> initializeRandomChromosome(int maxLength) {
    int length = random.nextInt(maxLength / 2) + maxLength / 4;
    List<Direction> chromosome = new ArrayList<>();
    Direction[] directions = Direction.values();

    for (int i = 0; i < length; i++) {
      chromosome.add(directions[random.nextInt(directions.length)]);
    }

    return chromosome;
  }

  private List<Direction> initializeHeuristicChromosome(Maze maze, int maxLength) {
    int length = random.nextInt(maxLength / 2) + maxLength / 4;
    List<Direction> chromosome = new ArrayList<>();
    Direction[] directions = Direction.values();
    Coordinate goal = maze.getGoal();
    Coordinate current = maze.getStart();

    for (int i = 0; i < length; i++) {
      if (random.nextDouble() < 0.6) {
        Direction bestDirection = selectBestDirection(maze, current, goal);
        chromosome.add(bestDirection);
        Coordinate next = move(current, bestDirection);
        if (isWalkable(maze, next)) {
          current = next;
        }
      } else {
        Direction randomDir = directions[random.nextInt(directions.length)];
        chromosome.add(randomDir);
        Coordinate next = move(current, randomDir);
        if (isWalkable(maze, next)) {
          current = next;
        }
      }
    }

    return chromosome;
  }

  private Direction selectBestDirection(Maze maze, Coordinate current, Coordinate goal) {
    Direction[] directions = Direction.values();
    Direction best = directions[random.nextInt(directions.length)];
    double bestScore = Double.MAX_VALUE;

    for (Direction dir : directions) {
      Coordinate next = move(current, dir);
      if (isWalkable(maze, next)) {
        int distance = manhattanDistance(next, goal);
        int cost = stepCost(maze, next);
        double score = distance * 2.0 + cost;
        if (score < bestScore) {
          bestScore = score;
          best = dir;
        }
      }
    }

    return best;
  }

  private List<Coordinate> executeChromosome(List<Direction> chromosome, Maze maze) {
    List<Coordinate> path = new ArrayList<>();
    Coordinate current = maze.getStart();
    path.add(current);

    for (Direction direction : chromosome) {
      Coordinate next = move(current, direction);
      if (isWalkable(maze, next)) {
        current = next;
        path.add(current);
        if (current.equals(maze.getGoal())) {
          break;
        }
      }
    }

    return path;
  }

  private void evaluateFitness(List<PathIndividual> population, Maze maze) {
    for (PathIndividual individual : population) {
      evaluateSingleFitness(individual, maze);
    }
  }

  private void evaluateSingleFitness(PathIndividual individual, Maze maze) {
    List<Coordinate> path = getCachedOrExecutePath(individual, maze);

    if (path.isEmpty()) {
      individual.fitness = Double.NEGATIVE_INFINITY;
      return;
    }

    Coordinate goal = maze.getGoal();
    Coordinate end = path.getLast();
    int distance = manhattanDistance(end, goal);
    int pathCost = calculatePathCost(maze, path);

    if (distance == 0) {
      individual.fitness = 1_000_000.0 - pathCost;
    } else {
      individual.fitness = 1_000_000.0 / (1.0 + distance * 10.0 + pathCost * 0.1);
    }
  }

  private List<Coordinate> getCachedOrExecutePath(PathIndividual individual, Maze maze) {
    if (individual.isCacheValid() && individual.cachedPath != null) {
      return individual.cachedPath;
    }

    List<Coordinate> path = executeChromosome(individual.chromosome, maze);
    individual.cachedPath = new ArrayList<>(path);
    return path;
  }

  private double calculateDiversity(List<PathIndividual> population) {
    if (population.size() < 2) {
      return 1.0;
    }

    Set<Integer> uniqueLengths = new HashSet<>();
    Set<String> uniqueChromosomes = new HashSet<>();

    for (PathIndividual individual : population) {
      uniqueLengths.add(individual.chromosome.size());
      uniqueChromosomes.add(chromosomeToString(individual.chromosome));
    }

    double lengthDiversity = (double) uniqueLengths.size() / population.size();
    double chromosomeDiversity = (double) uniqueChromosomes.size() / population.size();

    return (lengthDiversity + chromosomeDiversity) / 2.0;
  }

  private String chromosomeToString(List<Direction> chromosome) {
    StringBuilder sb = new StringBuilder();
    for (Direction dir : chromosome) {
      sb.append(dir.name().charAt(0));
    }
    return sb.toString();
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
      List<Coordinate> cachedPath = original.cachedPath != null
          ? new ArrayList<>(original.cachedPath)
          : null;
      elite.add(new PathIndividual(new ArrayList<>(original.chromosome), original.fitness, cachedPath));
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

  private List<Direction> crossoverChromosomes(List<Direction> parent1, List<Direction> parent2) {
    if (parent1.isEmpty() && parent2.isEmpty()) {
      return new ArrayList<>();
    }
    if (parent1.isEmpty()) {
      return new ArrayList<>(parent2);
    }
    if (parent2.isEmpty()) {
      return new ArrayList<>(parent1);
    }

    int crossoverType = random.nextInt(3);

    switch (crossoverType) {
      case 0 -> {
        return singlePointCrossover(parent1, parent2);
      }
      case 1 -> {
        return multiPointCrossover(parent1, parent2);
      }
      case 2 -> {
        return uniformCrossover(parent1, parent2);
      }
      default -> {
        return singlePointCrossover(parent1, parent2);
      }
    }
  }

  private List<Direction> singlePointCrossover(List<Direction> parent1, List<Direction> parent2) {
    int point1 = random.nextInt(parent1.size());
    int point2 = random.nextInt(parent2.size());

    List<Direction> child = new ArrayList<>();
    child.addAll(parent1.subList(0, point1));
    child.addAll(parent2.subList(point2, parent2.size()));

    return child;
  }

  private List<Direction> multiPointCrossover(List<Direction> parent1, List<Direction> parent2) {
    int numPoints = Math.min(3, Math.min(parent1.size(), parent2.size()) / 2);
    if (numPoints < 1) {
      return singlePointCrossover(parent1, parent2);
    }

    List<Integer> points1 = new ArrayList<>();
    List<Integer> points2 = new ArrayList<>();

    for (int i = 0; i < numPoints; i++) {
      points1.add(random.nextInt(parent1.size()));
      points2.add(random.nextInt(parent2.size()));
    }

    Collections.sort(points1);
    Collections.sort(points2);

    List<Direction> child = new ArrayList<>();
    boolean useParent1 = true;
    int prev1 = 0;
    int prev2 = 0;

    for (int i = 0; i < numPoints; i++) {
      if (useParent1) {
        child.addAll(parent1.subList(prev1, points1.get(i)));
        prev1 = points1.get(i);
      } else {
        child.addAll(parent2.subList(prev2, points2.get(i)));
        prev2 = points2.get(i);
      }
      useParent1 = !useParent1;
    }

    if (useParent1) {
      child.addAll(parent1.subList(prev1, parent1.size()));
    } else {
      child.addAll(parent2.subList(prev2, parent2.size()));
    }

    return child;
  }

  private List<Direction> uniformCrossover(List<Direction> parent1, List<Direction> parent2) {
    int maxLength = Math.max(parent1.size(), parent2.size());
    List<Direction> child = new ArrayList<>();

    for (int i = 0; i < maxLength; i++) {
      Direction gene;
      if (i >= parent1.size()) {
        gene = parent2.get(i);
      } else if (i >= parent2.size()) {
        gene = parent1.get(i);
      } else {
        gene = random.nextBoolean() ? parent1.get(i) : parent2.get(i);
      }
      child.add(gene);
    }

    return child;
  }

  private List<Direction> mutateChromosome(List<Direction> chromosome, int maxLength) {
    if (chromosome.isEmpty()) {
      return initializeRandomChromosome(maxLength);
    }

    List<Direction> mutated = new ArrayList<>(chromosome);
    int mutationType = random.nextInt(4);

    switch (mutationType) {
      case 0 -> {
        int index = random.nextInt(mutated.size());
        Direction[] directions = Direction.values();
        mutated.set(index, directions[random.nextInt(directions.length)]);
      }
      case 1 -> {
        if (mutated.size() < maxLength) {
          int index = random.nextInt(mutated.size() + 1);
          Direction[] directions = Direction.values();
          mutated.add(index, directions[random.nextInt(directions.length)]);
        }
      }
      case 2 -> {
        if (mutated.size() > 1) {
          int index = random.nextInt(mutated.size());
          mutated.remove(index);
        }
      }
      case 3 -> {
        if (mutated.size() > 1) {
          int index = random.nextInt(mutated.size() - 1);
          Direction temp = mutated.get(index);
          mutated.set(index, mutated.get(index + 1));
          mutated.set(index + 1, temp);
        }
      }
      default -> {
      }
    }

    return mutated;
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

  private static class PathIndividual {
    List<Direction> chromosome;
    double fitness;
    List<Coordinate> cachedPath;
    int chromosomeHash;

    PathIndividual(List<Direction> chromosome, double fitness) {
      this.chromosome = chromosome;
      this.fitness = fitness;
      this.cachedPath = null;
      this.chromosomeHash = computeChromosomeHash(chromosome);
    }

    PathIndividual(List<Direction> chromosome, double fitness, List<Coordinate> cachedPath) {
      this.chromosome = chromosome;
      this.fitness = fitness;
      this.cachedPath = cachedPath;
      this.chromosomeHash = computeChromosomeHash(chromosome);
    }

    boolean isCacheValid() {
      return cachedPath != null && chromosomeHash == computeChromosomeHash(chromosome);
    }

    void invalidateCache() {
      this.cachedPath = null;
      this.chromosomeHash = computeChromosomeHash(chromosome);
    }

    void updateChromosome(List<Direction> newChromosome) {
      this.chromosome = newChromosome;
      invalidateCache();
    }

    private static int computeChromosomeHash(List<Direction> chromosome) {
      if (chromosome == null || chromosome.isEmpty()) {
        return 0;
      }
      int hash = 1;
      for (Direction dir : chromosome) {
        hash = 31 * hash + dir.hashCode();
      }
      hash = 31 * hash + chromosome.size();
      return hash;
    }
  }
}
