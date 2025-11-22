package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages caching of best solutions per maze.
 *
 * Saves only the best solution found for each maze to avoid re-running
 * the genetic algorithm from scratch. Solutions are stored as simple
 * text files with coordinates.
 */
public final class GASolutionCache {

  private static final String CACHE_DIR = "ga_checkpoints";
  private static final String BEST_SOLUTION_SUFFIX = "_ga_best.txt";

  private GASolutionCache() {
  }

  /**
   * Saves the best solution for a maze.
   *
   * @param mazeName Name of the maze
   * @param path     Best path found
   * @return true if saved successfully
   */
  public static boolean saveBestSolution(String mazeName, List<Coordinate> path) {
    try {
      Path cacheDir = Paths.get(CACHE_DIR);
      if (!Files.exists(cacheDir)) {
        Files.createDirectories(cacheDir);
      }

      Path filePath = cacheDir.resolve(sanitizeFileName(mazeName) + BEST_SOLUTION_SUFFIX);

      try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
        for (Coordinate coord : path) {
          writer.write(coord.row() + "," + coord.column());
          writer.newLine();
        }
      }

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Loads the best solution for a maze if it exists.
   *
   * @param mazeName Name of the maze
   * @param maze     The maze instance to validate against
   * @return Best path if found and valid, null otherwise
   */
  public static List<Coordinate> loadBestSolution(String mazeName, Maze maze) {
    try {
      String fileName = sanitizeFileName(mazeName) + BEST_SOLUTION_SUFFIX;
      Path filePath = findCacheFile(fileName);

      if (filePath == null) {
        return null;
      }

      List<Coordinate> path = new ArrayList<>();
      try (BufferedReader reader = Files.newBufferedReader(filePath)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty()) {
            continue;
          }

          String[] parts = line.split(",");
          if (parts.length == 2) {
            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());
            path.add(new Coordinate(row, col));
          }
        }
      }

      if (isValidPath(path, maze) && path.size() > 1) {
        return path;
      }

      return null;
    } catch (IOException | NumberFormatException e) {
      return null;
    }
  }

  /**
   * Checks if a cached solution exists for the maze.
   */
  public static boolean hasCachedSolution(String mazeName) {
    String fileName = sanitizeFileName(mazeName) + BEST_SOLUTION_SUFFIX;

    Path filePath1 = Paths.get(CACHE_DIR, fileName);
    if (Files.exists(filePath1)) {
      return true;
    }

    Path filePath2 = Paths.get("app", CACHE_DIR, fileName);
    if (Files.exists(filePath2)) {
      return true;
    }

    Path filePath3 = Paths.get(System.getProperty("user.dir"), CACHE_DIR, fileName);
    if (Files.exists(filePath3)) {
      return true;
    }

    Path filePath4 = Paths.get(System.getProperty("user.dir"), "app", CACHE_DIR, fileName);
    return Files.exists(filePath4);
  }

  private static String sanitizeFileName(String name) {
    return name.replaceAll("[^a-zA-Z0-9_-]", "_");
  }

  private static Path findCacheFile(String fileName) {
    Path[] pathsToCheck = {
        Paths.get(CACHE_DIR, fileName),
        Paths.get("app", CACHE_DIR, fileName),
        Paths.get(System.getProperty("user.dir"), CACHE_DIR, fileName),
        Paths.get(System.getProperty("user.dir"), "app", CACHE_DIR, fileName)
    };

    for (Path path : pathsToCheck) {
      if (Files.exists(path)) {
        return path;
      }
    }
    return null;
  }

  private static boolean isValidPath(List<Coordinate> path, Maze maze) {
    if (path == null || path.isEmpty() || path.size() < 2) {
      return false;
    }

    if (!path.getFirst().equals(maze.getStart())) {
      return false;
    }

    for (Coordinate coord : path) {
      if (coord.row() < 0 || coord.row() >= maze.getHeight() ||
          coord.column() < 0 || coord.column() >= maze.getWidth()) {
        return false;
      }
      if (!maze.getCell(coord).isWalkable()) {
        return false;
      }
    }

    return true;
  }
}
