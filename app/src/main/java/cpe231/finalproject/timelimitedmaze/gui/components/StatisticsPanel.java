package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import com.raylib.Raylib;
import com.raylib.Colors;

public final class StatisticsPanel {
  public static final int LINE_HEIGHT = 30;
  private static final int TITLE_FONT_SIZE = 24;
  private static final int HEADING_FONT_SIZE = 20;
  private static final int TEXT_FONT_SIZE = 18;
  private static final int SMALL_FONT_SIZE = 16;

  public static void renderBackground(int panelX) {
    Raylib.DrawRectangle(panelX, 0, GUIConstants.STATS_PANEL_WIDTH, GUIConstants.WINDOW_HEIGHT,
        GUIConstants.STATS_PANEL_COLOR);
  }

  public static int renderTitle(int panelX, int startY, String title) {
    Raylib.DrawText(title, panelX + 10, startY, TITLE_FONT_SIZE, Colors.WHITE);
    return startY + LINE_HEIGHT + 10;
  }

  public static int renderMazeInfo(int panelX, int startY, Maze maze) {
    int currentY = startY;

    if (maze != null) {
      String mazeName = "Maze: " + maze.getName();
      Raylib.DrawText(mazeName, panelX + 10, currentY, TEXT_FONT_SIZE, Colors.LIGHTGRAY);
      currentY += LINE_HEIGHT;

      String dimensions = String.format("Size: %d x %d", maze.getWidth(), maze.getHeight());
      Raylib.DrawText(dimensions, panelX + 10, currentY, TEXT_FONT_SIZE, Colors.LIGHTGRAY);
      currentY += LINE_HEIGHT + 10;
    } else {
      String noMazeText = "Select a maze to begin";
      Raylib.DrawText(noMazeText, panelX + 10, currentY, TEXT_FONT_SIZE, Colors.LIGHTGRAY);
      currentY += LINE_HEIGHT + 20;
    }

    return currentY;
  }

  public static int renderErrorMessage(int panelX, int startY, String errorMessage) {
    if (errorMessage != null) {
      Raylib.DrawText(errorMessage, panelX + 10, startY, SMALL_FONT_SIZE, Colors.RED);
      return startY + LINE_HEIGHT + 10;
    }
    return startY;
  }

  public static int renderGAStatistics(int panelX, int startY, int generation, double fitness, int pathLength,
      boolean goalReached) {
    int currentY = startY;

    String generationText = "Generation: " + generation;
    Raylib.DrawText(generationText, panelX + 10, currentY, HEADING_FONT_SIZE, Colors.BLUE);
    currentY += LINE_HEIGHT;

    String fitnessText = String.format("Best Fitness: %.2f", fitness);
    Raylib.DrawText(fitnessText, panelX + 10, currentY, TEXT_FONT_SIZE, Colors.LIGHTGRAY);
    currentY += LINE_HEIGHT;

    String pathLengthText = "Path Length: " + pathLength;
    Raylib.DrawText(pathLengthText, panelX + 10, currentY, TEXT_FONT_SIZE, Colors.LIGHTGRAY);
    currentY += LINE_HEIGHT;

    String goalStatus = goalReached ? "Goal: REACHED" : "Goal: Not reached";
    Raylib.Color goalColor = goalReached ? Colors.GREEN : Colors.YELLOW;
    Raylib.DrawText(goalStatus, panelX + 10, currentY, TEXT_FONT_SIZE, goalColor);
    currentY += LINE_HEIGHT + 20;

    return currentY;
  }

  public static int renderSolverStatistics(int panelX, int startY, int pathLength, int totalCost, double executionTimeMs) {
    int currentY = startY;

    String pathLengthText = "Path Length: " + pathLength;
    Raylib.DrawText(pathLengthText, panelX + 10, currentY, HEADING_FONT_SIZE, Colors.BLUE);
    currentY += LINE_HEIGHT;

    String totalCostText = "Total Cost: " + totalCost;
    Raylib.DrawText(totalCostText, panelX + 10, currentY, HEADING_FONT_SIZE, Colors.BLUE);
    currentY += LINE_HEIGHT;

    String executionTime = String.format("Execution Time: %.3f ms", executionTimeMs);
    Raylib.DrawText(executionTime, panelX + 10, currentY, HEADING_FONT_SIZE, Colors.BLUE);
    currentY += LINE_HEIGHT + 20;

    return currentY;
  }
}
