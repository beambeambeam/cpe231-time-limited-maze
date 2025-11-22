package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.HashSet;
import java.util.Set;

public final class MazeRenderer {
  private MazeRenderer() {
  }

  public static void render(Maze maze, int cellSize, Set<Coordinate> pathSet) {
    if (maze == null) {
      return;
    }

    int offsetX = GUIConstants.PADDING;
    int offsetY = GUIConstants.PADDING;

    for (int row = 0; row < maze.getHeight(); row++) {
      for (int col = 0; col < maze.getWidth(); col++) {
        Coordinate coord = new Coordinate(row, col);
        MazeCell cell = maze.getCell(coord);
        int x = offsetX + col * cellSize;
        int y = offsetY + row * cellSize;

        MazeCellRenderer.renderCell(x, y, cellSize, cell, coord, pathSet);
      }
    }
  }

  public static void render(Maze maze, int cellSize, java.util.List<Coordinate> path) {
    Set<Coordinate> pathSet = path != null ? new HashSet<>(path) : new HashSet<>();
    render(maze, cellSize, pathSet);
  }
}
