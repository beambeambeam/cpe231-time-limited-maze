package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import cpe231.finalproject.timelimitedmaze.utils.MazeCellType;
import com.raylib.Raylib;
import com.raylib.Colors;
import com.raylib.Helpers;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MazeVisualizer {
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 800;
  private static final int STATS_PANEL_WIDTH = 300;
  private static final int MAZE_PANEL_WIDTH = WINDOW_WIDTH - STATS_PANEL_WIDTH;
  private static final int MAZE_PANEL_HEIGHT = WINDOW_HEIGHT;
  private static final int PADDING = 20;

  private final Maze maze;
  private final SolverResult result;
  private final Set<Coordinate> pathSet;
  private int cellSize;

  public MazeVisualizer(Maze maze, SolverResult result) {
    this.maze = maze;
    this.result = result;
    this.pathSet = new HashSet<>(result.path());
    calculateCellSize();
  }

  private void calculateCellSize() {
    int availableWidth = MAZE_PANEL_WIDTH - 2 * PADDING;
    int availableHeight = MAZE_PANEL_HEIGHT - 2 * PADDING;
    int cellSizeByWidth = availableWidth / maze.getWidth();
    int cellSizeByHeight = availableHeight / maze.getHeight();
    this.cellSize = Math.min(cellSizeByWidth, cellSizeByHeight);
    this.cellSize = Math.max(1, this.cellSize);
  }

  public void render() {
    renderMaze();
    renderPath();
    renderStartAndGoal();
    renderStatistics();
  }

  private void renderMaze() {
    int offsetX = PADDING;
    int offsetY = PADDING;

    for (int row = 0; row < maze.getHeight(); row++) {
      for (int col = 0; col < maze.getWidth(); col++) {
        Coordinate coord = new Coordinate(row, col);
        MazeCell cell = maze.getCell(coord);
        int x = offsetX + col * cellSize;
        int y = offsetY + row * cellSize;

        Raylib.Color cellColor = getCellColor(cell, coord);
        Raylib.DrawRectangle(x, y, cellSize, cellSize, cellColor);

        if (cellSize > 8 && cell.type() == MazeCellType.WEIGHTED && cell.weight().isPresent()) {
          String weightText = String.valueOf(cell.weight().getAsInt());
          int fontSize = Math.max(8, cellSize / 3);
          Raylib.DrawText(weightText, x + 2, y + 2, fontSize, Colors.BLACK);
        }
      }
    }
  }

  private Raylib.Color getCellColor(MazeCell cell, Coordinate coord) {
    if (pathSet.contains(coord)) {
      return Colors.BLUE;
    }

    return switch (cell.type()) {
      case WALL -> Colors.DARKGRAY;
      case START -> Colors.GREEN;
      case GOAL -> Colors.RED;
      case WEIGHTED -> {
        if (cell.weight().isPresent()) {
          int weight = cell.weight().getAsInt();
          int intensity = Math.min(255, 200 + (weight * 5));
          yield Helpers.newColor((byte) 240, (byte) intensity, (byte) 240, (byte) 255);
        }
        yield Colors.LIGHTGRAY;
      }
    };
  }

  private void renderPath() {
    List<Coordinate> path = result.path();
    if (path.size() < 2) {
      return;
    }

    int offsetX = PADDING;
    int offsetY = PADDING;
    int centerOffset = cellSize / 2;

    for (int i = 0; i < path.size() - 1; i++) {
      Coordinate from = path.get(i);
      Coordinate to = path.get(i + 1);

      int fromX = offsetX + from.column() * cellSize + centerOffset;
      int fromY = offsetY + from.row() * cellSize + centerOffset;
      int toX = offsetX + to.column() * cellSize + centerOffset;
      int toY = offsetY + to.row() * cellSize + centerOffset;

      Raylib.DrawLine(fromX, fromY, toX, toY, Helpers.newColor((byte) 0, (byte) 100, (byte) 255, (byte) 200));
    }
  }

  private void renderStartAndGoal() {
    int offsetX = PADDING;
    int offsetY = PADDING;
    int markerSize = Math.max(4, cellSize / 4);

    Coordinate start = maze.getStart();
    int startX = offsetX + start.column() * cellSize + cellSize / 2;
    int startY = offsetY + start.row() * cellSize + cellSize / 2;
    Raylib.DrawCircle(startX, startY, markerSize, Colors.GREEN);

    Coordinate goal = maze.getGoal();
    int goalX = offsetX + goal.column() * cellSize + cellSize / 2;
    int goalY = offsetY + goal.row() * cellSize + cellSize / 2;
    Raylib.DrawCircle(goalX, goalY, markerSize, Colors.RED);
  }

  private void renderStatistics() {
    int panelX = MAZE_PANEL_WIDTH;
    int panelY = PADDING;
    int lineHeight = 30;
    int currentY = panelY;

    Raylib.DrawRectangle(panelX, 0, STATS_PANEL_WIDTH, WINDOW_HEIGHT,
        Helpers.newColor((byte) 240, (byte) 240, (byte) 240, (byte) 255));

    String title = "Solver Statistics";
    Raylib.DrawText(title, panelX + 10, currentY, 24, Colors.BLACK);
    currentY += lineHeight + 10;

    String mazeName = "Maze: " + maze.getName();
    Raylib.DrawText(mazeName, panelX + 10, currentY, 18, Colors.DARKGRAY);
    currentY += lineHeight;

    String dimensions = String.format("Size: %d x %d", maze.getWidth(), maze.getHeight());
    Raylib.DrawText(dimensions, panelX + 10, currentY, 18, Colors.DARKGRAY);
    currentY += lineHeight + 10;

    String pathLength = "Path Length: " + result.path().size();
    Raylib.DrawText(pathLength, panelX + 10, currentY, 20, Colors.BLUE);
    currentY += lineHeight;

    String totalCost = "Total Cost: " + result.totalCost();
    Raylib.DrawText(totalCost, panelX + 10, currentY, 20, Colors.BLUE);
    currentY += lineHeight + 20;

    String legend = "Legend:";
    Raylib.DrawText(legend, panelX + 10, currentY, 18, Colors.BLACK);
    currentY += lineHeight;

    int legendItemSize = 15;
    int legendX = panelX + 20;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.GREEN);
    Raylib.DrawText("Start", legendX + legendItemSize + 5, currentY, 16, Colors.BLACK);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.RED);
    Raylib.DrawText("Goal", legendX + legendItemSize + 5, currentY, 16, Colors.BLACK);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.BLUE);
    Raylib.DrawText("Path", legendX + legendItemSize + 5, currentY, 16, Colors.BLACK);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.DARKGRAY);
    Raylib.DrawText("Wall", legendX + legendItemSize + 5, currentY, 16, Colors.BLACK);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.LIGHTGRAY);
    Raylib.DrawText("Walkable", legendX + legendItemSize + 5, currentY, 16, Colors.BLACK);
  }

  public int getWindowWidth() {
    return WINDOW_WIDTH;
  }

  public int getWindowHeight() {
    return WINDOW_HEIGHT;
  }
}
