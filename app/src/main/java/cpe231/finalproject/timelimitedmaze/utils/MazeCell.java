package cpe231.finalproject.timelimitedmaze.utils;

import java.util.Objects;
import java.util.OptionalInt;

public record MazeCell(MazeCellType type, String rawToken, OptionalInt weight) {

  public MazeCell {
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(rawToken, "rawToken cannot be null");
    Objects.requireNonNull(weight, "weight cannot be null");
    if (type == MazeCellType.WEIGHTED && weight.isEmpty()) {
      throw new IllegalArgumentException("Weighted cells must provide a numeric weight");
    }
    if (type != MazeCellType.WEIGHTED && weight.isPresent()) {
      throw new IllegalArgumentException("Non-weighted cells cannot define a weight");
    }
  }

  public boolean isWalkable() {
    return type != MazeCellType.WALL;
  }

  public int stepCost() {
    return weight.orElse(1);
  }
}
