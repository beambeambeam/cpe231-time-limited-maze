package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.solver.generic.FitnessCalculator;
import cpe231.finalproject.timelimitedmaze.solver.generic.Individual;
import cpe231.finalproject.timelimitedmaze.solver.generic.IntersectionCrossover;
import cpe231.finalproject.timelimitedmaze.solver.generic.MutationOperator;
import cpe231.finalproject.timelimitedmaze.solver.generic.PathChromosome;
import cpe231.finalproject.timelimitedmaze.solver.generic.Population;
import cpe231.finalproject.timelimitedmaze.solver.generic.PopulationInitializer;
import cpe231.finalproject.timelimitedmaze.solver.generic.TournamentSelection;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GARunner {
  private final Maze maze;
  private final FitnessCalculator fitnessCalculator;
  private final PopulationInitializer initializer;
  private final TournamentSelection selection;
  private final IntersectionCrossover crossover;
  private final MutationOperator mutation;
  private Population population;
  private int generation;
  private double bestFitness;
  private PathChromosome bestChromosome;

  public GARunner(Maze maze) {
    this.maze = maze;
    this.fitnessCalculator = new FitnessCalculator(maze);
    this.initializer = new PopulationInitializer(new Random());
    this.selection = new TournamentSelection(new Random());
    this.crossover = new IntersectionCrossover(new Random());
    this.mutation = new MutationOperator(new Random());
    this.generation = 0;
    this.bestFitness = Double.NEGATIVE_INFINITY;
    this.bestChromosome = null;
    initializePopulation();
  }

  private void initializePopulation() {
    List<PathChromosome> chromosomes = initializer.initialize(maze, GUIConstants.POPULATION_SIZE);
    List<Individual> individuals = new ArrayList<>();

    for (PathChromosome chromosome : chromosomes) {
      PathChromosome extended = chromosome.extendTowardGoal();
      double fitness = fitnessCalculator.calculateFitness(extended);
      individuals.add(new Individual(extended, fitness));
    }

    this.population = Population.from(individuals);
    this.population.sortByFitness();

    Individual best = population.getBest();
    this.bestFitness = best.fitness();
    this.bestChromosome = best.chromosome();
  }

  public GenerationResult getCurrentState() {
    List<Coordinate> bestPath = bestChromosome != null ? bestChromosome.getPath() : new ArrayList<>();
    boolean goalReached = bestChromosome != null && reachesGoal(bestPath, maze);
    List<List<Coordinate>> allPaths = new ArrayList<>();
    for (Individual individual : population.getIndividuals()) {
      allPaths.add(individual.chromosome().getPath());
    }
    return new GenerationResult(generation, bestPath, bestFitness, goalReached, allPaths);
  }

  public GenerationResult nextGeneration() {
    if (generation >= GUIConstants.MAX_GENERATIONS) {
      return getCurrentState();
    }

    List<Individual> newIndividuals = new ArrayList<>();

    List<Individual> elite = population.getElite(GUIConstants.ELITE_PERCENTAGE);
    newIndividuals.addAll(elite);

    while (newIndividuals.size() < GUIConstants.POPULATION_SIZE) {
      PathChromosome parent1 = selection.select(population.getIndividuals());
      PathChromosome parent2 = selection.select(population.getIndividuals());

      List<PathChromosome> offspring = crossover.crossover(parent1, parent2);

      for (PathChromosome child : offspring) {
        PathChromosome repaired = child.repair();
        PathChromosome mutated = mutation.mutate(repaired);
        PathChromosome finalChromosome = mutated.repair();
        double fitness = fitnessCalculator.calculateFitness(finalChromosome);
        newIndividuals.add(new Individual(finalChromosome, fitness));

        if (newIndividuals.size() >= GUIConstants.POPULATION_SIZE) {
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
    }

    generation++;

    List<Coordinate> bestPath = bestChromosome != null ? bestChromosome.getPath() : new ArrayList<>();
    boolean goalReached = bestChromosome != null && reachesGoal(bestPath, maze);
    List<List<Coordinate>> allPaths = new ArrayList<>();
    for (Individual individual : population.getIndividuals()) {
      allPaths.add(individual.chromosome().getPath());
    }

    return new GenerationResult(generation, bestPath, bestFitness, goalReached, allPaths);
  }

  private boolean reachesGoal(List<Coordinate> path, Maze maze) {
    if (path.isEmpty()) {
      return false;
    }
    return path.getLast().equals(maze.getGoal());
  }

  public record GenerationResult(int generation, List<Coordinate> bestPath, double bestFitness, boolean goalReached,
      List<List<Coordinate>> allPaths) {
  }
}
