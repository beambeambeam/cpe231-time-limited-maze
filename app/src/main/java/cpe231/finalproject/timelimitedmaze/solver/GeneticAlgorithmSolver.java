package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
  private BufferedWriter jsonLogWriter;
  private boolean isFirstJsonEntry = true;
  private static final DateTimeFormatter JSON_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

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
    String logFileName = "ga_log_" + System.currentTimeMillis() + ".json";
    try {
      jsonLogWriter = new BufferedWriter(new FileWriter(logFileName));
      jsonLogWriter.write("[\n");
      jsonLogWriter.flush();
      isFirstJsonEntry = true;
      log("JSON log file created: " + logFileName);
    } catch (IOException e) {
      log("Failed to create JSON log file: " + e.getMessage());
      jsonLogWriter = null;
    }

    try {

    int mazeSize = maze.getWidth() * maze.getHeight();
    int adaptivePopSize = Math.max(populationSize, (int) Math.sqrt(mazeSize) * 3);
    int adaptiveMaxGen = Math.max(maxGenerations, (int) Math.sqrt(mazeSize) * 5);
    int maxChromosomeLength = Math.max((maze.getWidth() + maze.getHeight()) * 4, mazeSize / 3);

    logJson("start", "GA start", new JsonBuilder()
        .add("mazeHeight", maze.getHeight())
        .add("mazeWidth", maze.getWidth())
        .add("populationSize", adaptivePopSize)
        .add("maxGenerations", adaptiveMaxGen)
        .add("maxChromosomeLength", maxChromosomeLength)
        .build());

    log("GA start grid " + maze.getHeight() + "x" + maze.getWidth()
        + " pop " + adaptivePopSize + " generations " + adaptiveMaxGen
        + " maxChromosomeLength " + maxChromosomeLength);

    logJson("phase", "Initializing population", new JsonBuilder()
        .add("populationSize", adaptivePopSize)
        .build());

    List<PathIndividual> population = initializePopulation(maze, maxChromosomeLength, adaptivePopSize);
    PathIndividual bestEver = null;

    logJson("phase", "Population initialized", new JsonBuilder()
        .add("actualPopulationSize", population.size())
        .build());

    for (int generation = 0; generation < adaptiveMaxGen; generation++) {
      logJson("generation_start", "Generation started", new JsonBuilder()
          .add("generation", generation)
          .add("maxGeneration", adaptiveMaxGen)
          .add("populationSize", population.size())
          .build());

      if (generation % 10 == 0 || generation == adaptiveMaxGen - 1) {
        log("GA progress: generation " + generation + "/" + adaptiveMaxGen);
      }

      long fitnessStartTime = System.nanoTime();
      evaluateFitness(population, maze);
      long fitnessEndTime = System.nanoTime();

      logJson("fitness_evaluated", "Fitness evaluation completed", new JsonBuilder()
          .add("generation", generation)
          .add("durationNs", fitnessEndTime - fitnessStartTime)
          .build());

      PathIndividual currentBest = getBest(population);
      List<Coordinate> bestPath = getCachedOrExecutePath(currentBest, maze);
      int distanceToGoal = bestPath.isEmpty() ? Integer.MAX_VALUE
          : manhattanDistance(bestPath.getLast(), maze.getGoal());
      int bestPathCost = bestPath.isEmpty() ? Integer.MAX_VALUE
          : calculatePathCost(maze, bestPath);
      boolean reachedGoal = reachesGoal(bestPath, maze);

      logJson("generation_best", "Best individual in generation", new JsonBuilder()
          .add("generation", generation)
          .add("fitness", currentBest.fitness)
          .add("pathLength", bestPath.size())
          .add("distanceToGoal", distanceToGoal)
          .add("pathCost", bestPathCost)
          .add("reachedGoal", reachedGoal)
          .add("chromosomeLength", currentBest.chromosome.size())
          .build());

      if (bestEver == null || currentBest.fitness > bestEver.fitness) {
        List<Coordinate> bestPathCopy = currentBest.cachedPath != null
            ? new ArrayList<>(currentBest.cachedPath)
            : executeChromosome(currentBest.chromosome, maze);
        bestEver = new PathIndividual(new ArrayList<>(currentBest.chromosome), currentBest.fitness, bestPathCopy);

        logJson("best_ever_updated", "Best ever individual updated", new JsonBuilder()
            .add("generation", generation)
            .add("fitness", bestEver.fitness)
            .add("pathLength", bestPathCopy.size())
            .add("reachedGoal", reachesGoal(bestPathCopy, maze))
            .build());
      }

      if (reachedGoal) {
        log("GA found goal at generation " + generation + " path length " + bestPath.size() + " (continuing to last generation)");
      } else if (distanceToGoal <= 5 && generation > adaptiveMaxGen * 0.8) {
        List<Coordinate> extendedPath = extendPathToGoal(bestPath, maze, maxChromosomeLength);
        if (reachesGoal(extendedPath, maze)) {
          log("GA extended path to goal at generation " + generation + " extended path length " + extendedPath.size());
          logJson("path_extended", "Path extended to goal", new JsonBuilder()
              .add("generation", generation)
              .add("originalLength", bestPath.size())
              .add("extendedLength", extendedPath.size())
              .build());
          bestEver = new PathIndividual(
              new ArrayList<>(currentBest.chromosome),
              currentBest.fitness + 1000000.0,
              extendedPath);
        }
      }

      double diversity = calculateDiversity(population);
      double mutationRate = adaptiveMutationRate(diversity, generation, adaptiveMaxGen);

      if (distanceToGoal > 0 && distanceToGoal < 10 && generation > adaptiveMaxGen * 0.7) {
        mutationRate = Math.min(0.7, mutationRate + 0.2);
      }

      logJson("generation_stats", "Generation statistics", new JsonBuilder()
          .add("generation", generation)
          .add("diversity", diversity)
          .add("mutationRate", mutationRate)
          .add("distanceToGoal", distanceToGoal)
          .build());

      if (generation % 5 == 0) {
        log("GA generation " + generation + " best path length " + bestPath.size()
            + " distance to goal " + distanceToGoal + " cost " + bestPathCost
            + " diversity " + String.format("%.3f", diversity) + " mutation " + String.format("%.3f", mutationRate));
      }

      int eliteCount = Math.max(2, (int) (adaptivePopSize * ELITE_PERCENTAGE));

      logJson("elite_selection_start", "Starting elite selection", new JsonBuilder()
          .add("generation", generation)
          .add("eliteCount", eliteCount)
          .build());

      List<PathIndividual> elite = getElite(population, eliteCount);

      logJson("elite_selected", "Elite selected", new JsonBuilder()
          .add("generation", generation)
          .add("eliteCount", elite.size())
          .build());

      List<PathIndividual> newPopulation = new ArrayList<>();
      for (PathIndividual eliteIndividual : elite) {
        newPopulation.add(new PathIndividual(
            new ArrayList<>(eliteIndividual.chromosome),
            eliteIndividual.fitness,
            eliteIndividual.cachedPath != null ? new ArrayList<>(eliteIndividual.cachedPath) : null));
      }

      logJson("crossover_start", "Starting crossover and mutation", new JsonBuilder()
          .add("generation", generation)
          .add("currentPopulationSize", newPopulation.size())
          .add("targetPopulationSize", adaptivePopSize)
          .build());

      int attempts = 0;
      int maxAttempts = adaptivePopSize * 10;
      int emptyChromosomeCount = 0;
      int crossoverCount = 0;
      int mutationCount = 0;

      while (newPopulation.size() < adaptivePopSize && attempts < maxAttempts) {
        attempts++;
        PathIndividual parent1 = selectParent(population);
        PathIndividual parent2 = selectParent(population);

        List<Direction> childChromosome = crossoverChromosomes(parent1.chromosome, parent2.chromosome);
        crossoverCount++;

        childChromosome = trimChromosome(childChromosome, maxChromosomeLength);

        boolean mutated = false;
        if (random.nextDouble() < mutationRate) {
          childChromosome = mutateChromosome(childChromosome, maxChromosomeLength);
          mutated = true;
          mutationCount++;
        }

        childChromosome = trimChromosome(childChromosome, maxChromosomeLength);

        if (!childChromosome.isEmpty()) {
          PathIndividual child = new PathIndividual(childChromosome, 0.0);
          newPopulation.add(child);
        } else {
          emptyChromosomeCount++;
          if (attempts > adaptivePopSize * 5) {
            List<Direction> fallbackChromosome = initializeRandomChromosome(maxChromosomeLength);
            PathIndividual child = new PathIndividual(fallbackChromosome, 0.0);
            newPopulation.add(child);
          }
        }

        if (attempts % 100 == 0) {
          logJson("crossover_progress", "Crossover progress", new JsonBuilder()
              .add("generation", generation)
              .add("attempts", attempts)
              .add("currentSize", newPopulation.size())
              .add("targetSize", adaptivePopSize)
              .add("emptyChromosomes", emptyChromosomeCount)
              .build());
        }
      }

      if (newPopulation.size() < adaptivePopSize) {
        logJson("crossover_fallback", "Using fallback to fill population", new JsonBuilder()
            .add("generation", generation)
            .add("currentSize", newPopulation.size())
            .add("targetSize", adaptivePopSize)
            .add("attempts", attempts)
            .build());

        while (newPopulation.size() < adaptivePopSize) {
          List<Direction> fallbackChromosome = initializeRandomChromosome(maxChromosomeLength);
          PathIndividual child = new PathIndividual(fallbackChromosome, 0.0);
          newPopulation.add(child);
        }
      }

      int maxChromosomeLengthInGen = 0;
      int avgChromosomeLength = 0;
      if (!newPopulation.isEmpty()) {
        for (PathIndividual ind : newPopulation) {
          maxChromosomeLengthInGen = Math.max(maxChromosomeLengthInGen, ind.chromosome.size());
          avgChromosomeLength += ind.chromosome.size();
        }
        avgChromosomeLength /= newPopulation.size();
      }

      logJson("crossover_complete", "Crossover and mutation completed", new JsonBuilder()
          .add("generation", generation)
          .add("finalPopulationSize", newPopulation.size())
          .add("totalAttempts", attempts)
          .add("emptyChromosomes", emptyChromosomeCount)
          .add("crossovers", crossoverCount)
          .add("mutations", mutationCount)
          .add("maxChromosomeLength", maxChromosomeLengthInGen)
          .add("avgChromosomeLength", avgChromosomeLength)
          .add("maxAllowedLength", maxChromosomeLength)
          .build());

      population = newPopulation;

      logJson("generation_end", "Generation completed", new JsonBuilder()
          .add("generation", generation)
          .build());
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
      log("GA failed to reach goal after " + adaptiveMaxGen + " generations, attempting path extension");
      List<Coordinate> extendedPath = extendPathToGoal(solution, maze, maxChromosomeLength);
      if (reachesGoal(extendedPath, maze)) {
        log("Path extension successful, final path length " + extendedPath.size());
        logJson("complete", "GA solve completed with path extension", new JsonBuilder()
            .add("finalPathLength", extendedPath.size())
            .add("finalPathCost", calculatePathCost(maze, extendedPath))
            .add("reachedGoal", true)
            .add("usedExtension", true)
            .build());
        closeJsonLog();
        return extendedPath;
      }
      log("GA failed to reach goal after " + adaptiveMaxGen + " generations");
      throw new MazeSolvingException("Genetic algorithm failed to find a solution");
    }

    log("GA final path length " + solution.size());

    logJson("complete", "GA solve completed", new JsonBuilder()
        .add("finalPathLength", solution.size())
        .add("finalPathCost", calculatePathCost(maze, solution))
        .add("reachedGoal", true)
        .build());

    closeJsonLog();
    return solution;
    } catch (Exception e) {
      logJson("error", "Exception occurred during solve", new JsonBuilder()
          .add("error", e.getClass().getSimpleName())
          .add("message", e.getMessage())
          .build());
      closeJsonLog();
      throw e;
    }
  }

  private void logJson(String eventType, String message, String jsonData) {
    if (jsonLogWriter == null) {
      return;
    }

    try {
      String timestamp = LocalDateTime.now().format(JSON_TIMESTAMP_FORMATTER);
      String prefix = isFirstJsonEntry ? "" : ",\n";
      isFirstJsonEntry = false;

      String jsonEntry = String.format(
          "%s  {\n    \"timestamp\": \"%s\",\n    \"event\": \"%s\",\n    \"message\": \"%s\",\n    \"data\": %s\n  }",
          prefix, timestamp, eventType, escapeJson(message), jsonData);

      jsonLogWriter.write(jsonEntry);
      jsonLogWriter.flush();
    } catch (IOException e) {
      log("Failed to write JSON log: " + e.getMessage());
    }
  }

  private void closeJsonLog() {
    if (jsonLogWriter != null) {
      try {
        jsonLogWriter.write("\n]");
        jsonLogWriter.close();
        log("JSON log saved successfully");
      } catch (IOException e) {
        log("Failed to close JSON log: " + e.getMessage());
      } finally {
        jsonLogWriter = null;
        isFirstJsonEntry = true;
      }
    }
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private static class JsonBuilder {
    private final StringBuilder sb = new StringBuilder();
    private boolean first = true;

    JsonBuilder add(String key, Object value) {
      if (!first) {
        sb.append(",\n      ");
      }
      first = false;
      sb.append("\"").append(key).append("\": ");
      if (value instanceof String) {
        sb.append("\"").append(escapeJson(value.toString())).append("\"");
      } else if (value instanceof Number) {
        sb.append(value);
      } else if (value instanceof Boolean) {
        sb.append(value);
      } else {
        sb.append("\"").append(escapeJson(value.toString())).append("\"");
      }
      return this;
    }

    String build() {
      return "{\n      " + sb.toString() + "\n    }";
    }

    private String escapeJson(String str) {
      return str.replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "\\r")
          .replace("\t", "\\t");
    }
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
    int minLength = Math.max(maxLength / 4, 10);
    int length = random.nextInt(maxLength - minLength + 1) + minLength;
    List<Direction> chromosome = new ArrayList<>();
    Direction[] directions = Direction.values();

    for (int i = 0; i < length; i++) {
      chromosome.add(directions[random.nextInt(directions.length)]);
    }

    return chromosome;
  }

  private List<Direction> initializeHeuristicChromosome(Maze maze, int maxLength) {
    int minLength = Math.max(maxLength / 4, 10);
    int length = random.nextInt(maxLength - minLength + 1) + minLength;
    List<Direction> chromosome = new ArrayList<>();
    Direction[] directions = Direction.values();
    Coordinate goal = maze.getGoal();
    Coordinate current = maze.getStart();

    for (int i = 0; i < length; i++) {
      if (random.nextDouble() < 0.7) {
        Direction bestDirection = selectBestDirection(maze, current, goal);
        chromosome.add(bestDirection);
        Coordinate next = move(current, bestDirection);
        if (isWalkable(maze, next)) {
          current = next;
          if (current.equals(goal)) {
            break;
          }
        }
      } else {
        Direction randomDir = directions[random.nextInt(directions.length)];
        chromosome.add(randomDir);
        Coordinate next = move(current, randomDir);
        if (isWalkable(maze, next)) {
          current = next;
          if (current.equals(goal)) {
            break;
          }
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
      double distanceReward = 1_000_000.0 / (1.0 + distance * 5.0);
      double costPenalty = pathCost * 0.05;
      double lengthBonus = distance < 30 ? (30 - distance) * 1000.0 : 0.0;
      individual.fitness = distanceReward - costPenalty + lengthBonus;
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
    double baseRate = BASE_MUTATION_RATE + diversityFactor * 0.25;
    if (diversity < 0.3) {
      baseRate += 0.2;
    }
    return Math.min(0.6, baseRate + progressFactor * 0.1);
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
    List<Direction> result;

    switch (crossoverType) {
      case 0 -> {
        result = singlePointCrossover(parent1, parent2);
      }
      case 1 -> {
        result = multiPointCrossover(parent1, parent2);
      }
      case 2 -> {
        result = uniformCrossover(parent1, parent2);
      }
      default -> {
        result = singlePointCrossover(parent1, parent2);
      }
    }

    return result;
  }

  private List<Direction> trimChromosome(List<Direction> chromosome, int maxLength) {
    if (chromosome.size() <= maxLength) {
      return chromosome;
    }
    return new ArrayList<>(chromosome.subList(0, maxLength));
  }

  private List<Direction> singlePointCrossover(List<Direction> parent1, List<Direction> parent2) {
    if (parent1.isEmpty() || parent2.isEmpty()) {
      return parent1.isEmpty() ? new ArrayList<>(parent2) : new ArrayList<>(parent1);
    }

    int point1 = random.nextInt(parent1.size()) + 1;
    int point2 = random.nextInt(parent2.size());

    List<Direction> child = new ArrayList<>();
    child.addAll(parent1.subList(0, point1));
    child.addAll(parent2.subList(point2, parent2.size()));

    if (child.isEmpty()) {
      return new ArrayList<>(parent1);
    }
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
        if (!mutated.isEmpty()) {
          int index = random.nextInt(mutated.size());
          Direction[] directions = Direction.values();
          mutated.set(index, directions[random.nextInt(directions.length)]);
        }
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

    if (mutated.size() > maxLength) {
      mutated = new ArrayList<>(mutated.subList(0, maxLength));
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

  private List<Coordinate> extendPathToGoal(List<Coordinate> path, Maze maze, int maxLength) {
    if (path.isEmpty()) {
      return path;
    }

    Coordinate current = path.getLast();
    Coordinate goal = maze.getGoal();
    List<Coordinate> extended = new ArrayList<>(path);
    Set<Coordinate> visited = new HashSet<>(path);
    int maxSteps = Math.min(maxLength - path.size(), 500);

    for (int step = 0; step < maxSteps && !current.equals(goal); step++) {
      Direction bestDir = null;
      int bestDistance = Integer.MAX_VALUE;

      for (Direction dir : Direction.values()) {
        Coordinate next = move(current, dir);
        if (isWalkable(maze, next) && !visited.contains(next)) {
          int dist = manhattanDistance(next, goal);
          if (dist < bestDistance) {
            bestDistance = dist;
            bestDir = dir;
          }
        }
      }

      if (bestDir == null) {
        for (Direction dir : Direction.values()) {
          Coordinate next = move(current, dir);
          if (isWalkable(maze, next)) {
            int dist = manhattanDistance(next, goal);
            if (dist < bestDistance) {
              bestDistance = dist;
              bestDir = dir;
            }
          }
        }
      }

      if (bestDir != null) {
        Coordinate next = move(current, bestDir);
        if (isWalkable(maze, next)) {
          current = next;
          extended.add(current);
          visited.add(current);
        } else {
          break;
        }
      } else {
        break;
      }
    }

    return extended;
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
