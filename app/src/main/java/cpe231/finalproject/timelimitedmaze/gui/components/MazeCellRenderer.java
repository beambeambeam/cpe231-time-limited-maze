package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import cpe231.finalproject.timelimitedmaze.utils.MazeCellType;
import com.raylib.Raylib;
import com.raylib.Colors;
import com.raylib.Helpers;
import java.util.Set;

public final class MazeCellRenderer {
  private MazeCellRenderer() {
  }

  public static Raylib.Color getCellColor(MazeCell cell, Coordinate coord, Set<Coordinate> pathSet) {
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

  public static void renderCell(int x, int y, int cellSize, MazeCell cell, Coordinate coord, Set<Coordinate> pathSet) {
    Raylib.Color cellColor = getCellColor(cell, coord, pathSet);
    Raylib.DrawRectangle(x, y, cellSize, cellSize, cellColor);

    if (cellSize > 8 && cell.type() == MazeCellType.WEIGHTED && cell.weight().isPresent()) {
      String weightText = String.valueOf(cell.weight().getAsInt());
      int fontSize = Math.max(8, cellSize / 3);
      Raylib.DrawText(weightText, x + 2, y + 2, fontSize, Colors.BLACK);
    }
  }
}
