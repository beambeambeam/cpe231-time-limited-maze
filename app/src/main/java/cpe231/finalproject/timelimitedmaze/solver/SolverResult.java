package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import java.util.List;
import java.util.Objects;

public record SolverResult(List<Coordinate> path, int totalCost, long startTimeMs, long endTimeMs) {

  public SolverResult {
    Objects.requireNonNull(path, "path cannot be null");
    if (path.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be empty");
    }
    if (endTimeMs < startTimeMs) {
      throw new IllegalArgumentException("endTimeMs must be >= startTimeMs");
    }
  }
}
