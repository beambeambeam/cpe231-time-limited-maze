package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.MazeRenderer;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.List;

public final class GAMazeRenderer {
  private GAMazeRenderer() {
  }

  public static void render(Maze maze, int cellSize, List<Coordinate> path) {
    MazeRenderer.render(maze, cellSize, path);
  }
}
