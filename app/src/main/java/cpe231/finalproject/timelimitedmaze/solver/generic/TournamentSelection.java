package cpe231.finalproject.timelimitedmaze.solver.generic;

import java.util.List;
import java.util.Random;

/**
 * Tournament selection operator for genetic algorithm.
 *
 * Selection is the process of choosing parents for reproduction. Tournament
 * selection
 * randomly selects a small group (tournament) of individuals and picks the best
 * one.
 * This creates selection pressure toward better solutions while maintaining
 * some
 * randomness, preventing premature convergence to local optima.
 *
 * Tournament size of 4 provides a good balance between selection pressure and
 * diversity.
 */
public final class TournamentSelection {

  private static final int TOURNAMENT_SIZE = 4;
  private final Random random;

  public TournamentSelection(Random random) {
    this.random = random;
  }

  /**
   * Selects a parent from the population using tournament selection.
   *
   * @param population List of individuals with their fitness scores
   * @return Selected PathChromosome
   */
  public PathChromosome select(List<Individual> population) {
    if (population.isEmpty()) {
      throw new IllegalArgumentException("Population cannot be empty");
    }

    Individual best = null;
    double bestFitness = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < TOURNAMENT_SIZE && i < population.size(); i++) {
      Individual candidate = population.get(random.nextInt(population.size()));
      if (candidate.fitness() > bestFitness) {
        bestFitness = candidate.fitness();
        best = candidate;
      }
    }

    return best != null ? best.chromosome() : population.get(0).chromosome();
  }
}
