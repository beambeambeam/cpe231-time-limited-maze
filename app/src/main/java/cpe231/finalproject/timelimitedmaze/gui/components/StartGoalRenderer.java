package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import com.raylib.Raylib;
import com.raylib.Colors;

public final class StartGoalRenderer {
  private StartGoalRenderer() {
  }

  public static void render(Maze maze, int cellSize) {
    if (maze == null) {
      return;
    }

    int offsetX = GUIConstants.PADDING;
    int offsetY = GUIConstants.PADDING;
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
}
