package cpe231.finalproject.timelimitedmaze.utils;

import java.io.UncheckedIOException;

public final class MazeValidator {
  private MazeValidator() {
  }

  public static boolean isValidMaze(String fileName) {
    try {
      MazeStore.getMaze(fileName);
      return true;
    } catch (IllegalArgumentException | UncheckedIOException e) {
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}

