package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.PathRenderer;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import com.raylib.Raylib;
import com.raylib.Helpers;
import java.util.List;

public final class GAPathRenderer {
  private static final int POPULATION_PATH_OPACITY = 80;
  private static final int BEST_PATH_OPACITY = 200;

  private GAPathRenderer() {
  }

  public static void render(List<Coordinate> bestPath, List<List<Coordinate>> allPaths, int cellSize) {
    if (allPaths == null || allPaths.isEmpty()) {
      if (bestPath != null && !bestPath.isEmpty()) {
        PathRenderer.render(bestPath, cellSize);
      }
      return;
    }

    int offsetX = cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants.PADDING;
    int offsetY = cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants.PADDING;
    int centerOffset = cellSize / 2;

    Raylib.Color populationPathColor = Helpers.newColor((byte) 0, (byte) 100, (byte) 255, (byte) POPULATION_PATH_OPACITY);
    Raylib.Color bestPathColor = Helpers.newColor((byte) 0, (byte) 100, (byte) 255, (byte) BEST_PATH_OPACITY);

    for (List<Coordinate> path : allPaths) {
      if (path == null || path.size() < 2) {
        continue;
      }

      boolean isBestPath = path.equals(bestPath);
      Raylib.Color pathColor = isBestPath ? bestPathColor : populationPathColor;

      for (int i = 0; i < path.size() - 1; i++) {
        Coordinate from = path.get(i);
        Coordinate to = path.get(i + 1);

        int fromX = offsetX + from.column() * cellSize + centerOffset;
        int fromY = offsetY + from.row() * cellSize + centerOffset;
        int toX = offsetX + to.column() * cellSize + centerOffset;
        int toY = offsetY + to.row() * cellSize + centerOffset;

        Raylib.DrawLine(fromX, fromY, toX, toY, pathColor);
      }
    }
  }
}
