package cpe231.finalproject.timelimitedmaze.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class MazeReader {

  private static final String MAZE_FOLDER = "maze/";

  private MazeReader() {
  }

  public static List<String> readMaze(String fileName) {
    Objects.requireNonNull(fileName, "fileName cannot be null");

    String resourcePath = MAZE_FOLDER + fileName;

    try (InputStream inputStream = MazeReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Maze file not found in resources: " + resourcePath);
      }

      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        return reader.lines().filter(line -> !line.isBlank()).toList();
      }
    } catch (IOException exception) {
      throw new UncheckedIOException("Failed to read maze file: " + resourcePath, exception);
    }
  }
}
