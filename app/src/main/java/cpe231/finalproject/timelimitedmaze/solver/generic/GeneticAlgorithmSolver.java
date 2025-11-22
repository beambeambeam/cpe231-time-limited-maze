package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genetic Algorithm solver for mazes.
 *
 * This solver uses evolutionary computation to find paths through mazes. The
 * algorithm
 * maintains a population of candidate solutions (paths), evaluates their
 * fitness,
 * selects parents, creates offspring through crossover and mutation, and
 * evolves
 * the population over generations until a solution is found or termination
 * criteria
 * are met.
 *
 * Key features:
 * - Variable-length path encoding (chromosomes can expand/contract)
 * - Ramped half-and-half initialization (mix of random and goal-biased paths)
 * - Multi-objective fitness (distance to goal, then cost minimization)
 * - Tournament selection, intersection crossover, three mutation types
 * - Elitism to preserve best solutions
 * - Checkpointing to save/load state
 */
public final class GeneticAlgorithmSolver extends MazeSolver {

  private static final int DEFAULT_POPULATION_SIZE = 500;
  private static final int DEFAULT_MAX_GENERATIONS = 200;
  private static final int STAGNATION_THRESHOLD = 20;
  private static final double ELITE_PERCENTAGE = 0.05;

  private final int populationSize;
  private final int maxGenerations;
  private final Random random;
  private final boolean useCache;

  public GeneticAlgorithmSolver() {
    this(DEFAULT_POPULATION_SIZE, DEFAULT_MAX_GENERATIONS, new Random(), true);
  }

  public GeneticAlgorithmSolver(int populationSize, int maxGenerations, Random random, boolean useCache) {
    if (populationSize < 10) {
      throw new IllegalArgumentException("Population size must be at least 10");
    }
    if (maxGenerations < 1) {
      throw new IllegalArgumentException("Max generations must be at least 1");
    }
    this.populationSize = populationSize;
    this.maxGenerations = maxGenerations;
    this.random = random != null ? random : new Random();
    this.useCache = useCache;
  }

  @Override
  public String getAlgorithmName() {
    return "Genetic Algorithm";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    if (useCache) {
      List<Coordinate> cached = GASolutionCache.loadBestSolution(maze.getName(), maze);
      if (cached != null && reachesGoal(cached, maze) && cached.size() > 10) {
        return cached;
      }
    }

    FitnessCalculator fitnessCalculator = new FitnessCalculator(maze);
    PopulationInitializer initializer = new PopulationInitializer(random);
    TournamentSelection selection = new TournamentSelection(random);
    IntersectionCrossover crossover = new IntersectionCrossover(random);
    MutationOperator mutation = new MutationOperator(random);

    Population population = initializePopulation(maze, initializer, fitnessCalculator);

    double bestFitness = Double.NEGATIVE_INFINITY;
    int stagnationCount = 0;
    PathChromosome bestChromosome = null;

    for (int generation = 0; generation < maxGenerations; generation++) {
      List<Individual> newIndividuals = new ArrayList<>();

      List<Individual> elite = population.getElite(ELITE_PERCENTAGE);
      newIndividuals.addAll(elite);

      while (newIndividuals.size() < populationSize) {
        PathChromosome parent1 = selection.select(population.getIndividuals());
        PathChromosome parent2 = selection.select(population.getIndividuals());

        List<PathChromosome> offspring = crossover.crossover(parent1, parent2);

        for (PathChromosome child : offspring) {
          PathChromosome repaired = child.repair();
          PathChromosome mutated = mutation.mutate(repaired);
          PathChromosome finalChromosome = mutated.repair();
          double fitness = fitnessCalculator.calculateFitness(finalChromosome);
          newIndividuals.add(new Individual(finalChromosome, fitness));

          if (newIndividuals.size() >= populationSize) {
            break;
          }
        }
      }

      population = Population.from(newIndividuals);
      population.sortByFitness();

      Individual currentBest = population.getBest();
      if (currentBest.fitness() > bestFitness) {
        bestFitness = currentBest.fitness();
        bestChromosome = currentBest.chromosome();
        stagnationCount = 0;
      } else {
        stagnationCount++;
      }

      if (reachesGoal(currentBest.chromosome().getPath(), maze)) {
        List<Coordinate> solution = currentBest.chromosome().getPath();
        if (useCache && solution.size() > 1) {
          GASolutionCache.saveBestSolution(maze.getName(), solution);
        }
        return solution;
      }

      if (stagnationCount >= STAGNATION_THRESHOLD) {
        break;
      }

      if (useCache && generation % 10 == 0) {
        GACheckpointManager.saveCheckpoint(maze.getName(), population, generation);
      }
    }

    if (bestChromosome != null) {
      List<Coordinate> solution = bestChromosome.getPath();
      if (useCache && solution.size() > 1) {
        GASolutionCache.saveBestSolution(maze.getName(), solution);
      }
      return solution;
    }

    Individual best = population.getBest();
    List<Coordinate> solution = best.chromosome().getPath();
    if (useCache && solution.size() > 1) {
      GASolutionCache.saveBestSolution(maze.getName(), solution);
    }
    return solution;
  }

  private Population initializePopulation(Maze maze, PopulationInitializer initializer,
      FitnessCalculator fitnessCalculator) {
    if (useCache) {
      GACheckpointManager.CheckpointData checkpoint = GACheckpointManager.loadCheckpoint(maze.getName(), maze);
      if (checkpoint != null) {
        List<Individual> individuals = new ArrayList<>();
        for (Individual ind : checkpoint.population().getIndividuals()) {
          double fitness = fitnessCalculator.calculateFitness(ind.chromosome());
          individuals.add(new Individual(ind.chromosome(), fitness));
        }
        return Population.from(individuals);
      }
    }

    List<PathChromosome> chromosomes = initializer.initialize(maze, populationSize);
    List<Individual> individuals = new ArrayList<>();

    for (PathChromosome chromosome : chromosomes) {
      PathChromosome extended = chromosome.extendTowardGoal();
      double fitness = fitnessCalculator.calculateFitness(extended);
      individuals.add(new Individual(extended, fitness));
    }

    return Population.from(individuals);
  }

  private boolean reachesGoal(List<Coordinate> path, Maze maze) {
    if (path.isEmpty()) {
      return false;
    }
    return path.getLast().equals(maze.getGoal());
  }
}
