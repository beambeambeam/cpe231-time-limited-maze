package cpe231.finalproject.timelimitedmaze.solver.generic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages a population of individuals in the genetic algorithm.
 *
 * The population is the collection of candidate solutions (chromosomes) being
 * evolved.
 * This class handles sorting by fitness, selecting individuals, and applying
 * elitism
 * to preserve the best solutions across generations.
 */
public final class Population {

  private final List<Individual> individuals;

  public Population(List<Individual> individuals) {
    this.individuals = new ArrayList<>(individuals);
  }

  public List<Individual> getIndividuals() {
    return List.copyOf(individuals);
  }

  public int size() {
    return individuals.size();
  }

  public boolean isEmpty() {
    return individuals.isEmpty();
  }

  /**
   * Sorts individuals by fitness in descending order (best first).
   */
  public void sortByFitness() {
    individuals.sort(Comparator.comparingDouble(Individual::fitness).reversed());
  }

  /**
   * Gets the best individual (highest fitness).
   */
  public Individual getBest() {
    if (individuals.isEmpty()) {
      throw new IllegalStateException("Population is empty");
    }
    return individuals.stream()
        .max(Comparator.comparingDouble(Individual::fitness))
        .orElseThrow();
  }

  /**
   * Applies elitism by preserving the top percentage of individuals.
   *
   * @param elitePercentage Percentage of top individuals to preserve (0.0 to 1.0)
   * @return List of elite individuals
   */
  public List<Individual> getElite(double elitePercentage) {
    sortByFitness();
    int eliteCount = Math.max(1, (int) (individuals.size() * elitePercentage));
    return new ArrayList<>(individuals.subList(0, Math.min(eliteCount, individuals.size())));
  }

  /**
   * Adds an individual to the population.
   */
  public void add(Individual individual) {
    individuals.add(individual);
  }

  /**
   * Creates a new population from a list of individuals.
   */
  public static Population from(List<Individual> individuals) {
    return new Population(individuals);
  }
}
