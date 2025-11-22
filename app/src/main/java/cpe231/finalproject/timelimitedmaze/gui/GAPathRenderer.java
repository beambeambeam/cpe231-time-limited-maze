package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.PathRenderer;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import java.util.List;

public final class GAPathRenderer {
  private GAPathRenderer() {
  }

  public static void render(List<Coordinate> path, int cellSize) {
    PathRenderer.render(path, cellSize);
  }
}
