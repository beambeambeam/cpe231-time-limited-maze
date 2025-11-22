package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import com.raylib.Raylib;
import java.util.List;

public final class PathRenderer {
  private PathRenderer() {
  }

  public static void render(List<Coordinate> path, int cellSize) {
    if (path == null || path.size() < 2) {
      return;
    }

    int offsetX = GUIConstants.PADDING;
    int offsetY = GUIConstants.PADDING;
    int centerOffset = cellSize / 2;

    for (int i = 0; i < path.size() - 1; i++) {
      Coordinate from = path.get(i);
      Coordinate to = path.get(i + 1);

      int fromX = offsetX + from.column() * cellSize + centerOffset;
      int fromY = offsetY + from.row() * cellSize + centerOffset;
      int toX = offsetX + to.column() * cellSize + centerOffset;
      int toY = offsetY + to.row() * cellSize + centerOffset;

      Raylib.DrawLine(fromX, fromY, toX, toY, GUIConstants.PATH_LINE_COLOR);
    }
  }
}
