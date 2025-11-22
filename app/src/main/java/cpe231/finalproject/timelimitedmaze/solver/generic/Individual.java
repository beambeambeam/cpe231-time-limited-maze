package cpe231.finalproject.timelimitedmaze.solver.generic;

/**
 * Represents an individual in the genetic algorithm population.
 * Associates a chromosome (path) with its fitness score.
 */
public record Individual(PathChromosome chromosome, double fitness) {

  public Individual {
    if (chromosome == null) {
      throw new IllegalArgumentException("chromosome cannot be null");
    }
  }
}
