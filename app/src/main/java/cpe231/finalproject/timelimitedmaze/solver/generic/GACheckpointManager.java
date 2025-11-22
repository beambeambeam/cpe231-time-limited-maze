package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages checkpointing of full population state for genetic algorithm.
 *
 * Saves the entire population state (chromosomes + fitness) to allow resuming
 * evolution from a previous point. Uses JSON-like format for human readability.
 * This enables continuing evolution without starting from scratch.
 */
public final class GACheckpointManager {

  private static final String CACHE_DIR = "ga_checkpoints";
  private static final String CHECKPOINT_SUFFIX = "_ga_checkpoint.json";

  private GACheckpointManager() {
  }

  /**
   * Saves a checkpoint of the population state.
   *
   * @param mazeName   Name of the maze
   * @param population Current population
   * @param generation Current generation number
   * @return true if saved successfully
   */
  public static boolean saveCheckpoint(String mazeName, Population population, int generation) {
    try {
      Path cacheDir = Paths.get(CACHE_DIR);
      if (!Files.exists(cacheDir)) {
        Files.createDirectories(cacheDir);
      }

      Path filePath = cacheDir.resolve(sanitizeFileName(mazeName) + CHECKPOINT_SUFFIX);

      try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
        writer.write("{");
        writer.newLine();
        writer.write("  \"mazeName\": \"" + escapeJson(mazeName) + "\",");
        writer.newLine();
        writer.write("  \"generation\": " + generation + ",");
        writer.newLine();
        writer.write("  \"populationSize\": " + population.size() + ",");
        writer.newLine();
        writer.write("  \"individuals\": [");
        writer.newLine();

        List<Individual> individuals = population.getIndividuals();
        for (int i = 0; i < individuals.size(); i++) {
          Individual ind = individuals.get(i);
          writer.write("    {");
          writer.newLine();
          writer.write("      \"fitness\": " + ind.fitness() + ",");
          writer.newLine();
          writer.write("      \"path\": [");
          writer.newLine();

          List<Coordinate> path = ind.chromosome().getPath();
          for (int j = 0; j < path.size(); j++) {
            Coordinate coord = path.get(j);
            writer.write("        [" + coord.row() + ", " + coord.column() + "]");
            if (j < path.size() - 1) {
              writer.write(",");
            }
            writer.newLine();
          }

          writer.write("      ]");
          writer.newLine();
          writer.write("    }");
          if (i < individuals.size() - 1) {
            writer.write(",");
          }
          writer.newLine();
        }

        writer.write("  ]");
        writer.newLine();
        writer.write("}");
      }

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Loads a checkpoint if it exists and is valid for the maze.
   *
   * @param mazeName Name of the maze
   * @param maze     The maze instance to validate against
   * @return CheckpointData if found and valid, null otherwise
   */
  public static CheckpointData loadCheckpoint(String mazeName, Maze maze) {
    try {
      Path filePath = Paths.get(CACHE_DIR, sanitizeFileName(mazeName) + CHECKPOINT_SUFFIX);

      if (!Files.exists(filePath)) {
        return null;
      }

      List<String> lines = Files.readAllLines(filePath);
      String content = String.join("\n", lines);

      int generationStart = content.indexOf("\"generation\":") + 13;
      int generationEnd = content.indexOf(",", generationStart);
      if (generationEnd == -1) {
        generationEnd = content.indexOf("}", generationStart);
      }
      int generation = Integer.parseInt(content.substring(generationStart, generationEnd).trim());

      List<PathChromosome> chromosomes = new ArrayList<>();
      List<Double> fitnesses = new ArrayList<>();

      int pathStart = content.indexOf("\"path\": [");
      while (pathStart != -1) {
        int fitnessStart = content.lastIndexOf("\"fitness\":", pathStart) + 10;
        int fitnessEnd = content.indexOf(",", fitnessStart);
        if (fitnessEnd == -1) {
          fitnessEnd = content.indexOf("}", fitnessStart);
        }
        double fitness = Double.parseDouble(content.substring(fitnessStart, fitnessEnd).trim());

        int coordStart = pathStart + 9;
        int coordEnd = content.indexOf("]", coordStart);
        List<Coordinate> path = new ArrayList<>();

        String pathSection = content.substring(coordStart, coordEnd);
        String[] coordStrings = pathSection.split("\\[");
        for (String coordStr : coordStrings) {
          coordStr = coordStr.trim();
          if (coordStr.isEmpty() || !coordStr.contains(",")) {
            continue;
          }
          coordStr = coordStr.replace("]", "").replace(",", "").trim();
          String[] parts = coordStr.split("\\s+");
          if (parts.length >= 2) {
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            path.add(new Coordinate(row, col));
          }
        }

        if (!path.isEmpty()) {
          PathChromosome chromosome = new PathChromosome(path, maze);
          chromosomes.add(chromosome);
          fitnesses.add(fitness);
        }

        pathStart = content.indexOf("\"path\": [", pathStart + 1);
      }

      if (chromosomes.isEmpty()) {
        return null;
      }

      List<Individual> individuals = new ArrayList<>();
      for (int i = 0; i < chromosomes.size(); i++) {
        individuals.add(new Individual(chromosomes.get(i), fitnesses.get(i)));
      }

      return new CheckpointData(Population.from(individuals), generation);
    } catch (IOException | NumberFormatException | StringIndexOutOfBoundsException e) {
      return null;
    }
  }

  /**
   * Checks if a checkpoint exists for the maze.
   */
  public static boolean hasCheckpoint(String mazeName) {
    Path filePath = Paths.get(CACHE_DIR, sanitizeFileName(mazeName) + CHECKPOINT_SUFFIX);
    return Files.exists(filePath);
  }

  private static String sanitizeFileName(String name) {
    return name.replaceAll("[^a-zA-Z0-9_-]", "_");
  }

  private static String escapeJson(String str) {
    return str.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /**
   * Data class for checkpoint information.
   */
  public record CheckpointData(Population population, int generation) {
  }
}
