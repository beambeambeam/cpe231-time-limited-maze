package cpe231.finalproject.timelimitedmaze.gui.components;

import com.raylib.Raylib;
import com.raylib.Colors;

public final class LegendPanel {
  private static final int LEGEND_ITEM_SIZE = 15;
  private static final int LEGEND_INDENT = 20;

  public static void render(int panelX, int startY) {
    int lineHeight = 30;
    int currentY = startY;

    String legend = "Legend:";
    Raylib.DrawText(legend, panelX + 10, currentY, 18, Colors.WHITE);
    currentY += lineHeight;

    int legendX = panelX + LEGEND_INDENT;

    Raylib.DrawRectangle(legendX, currentY, LEGEND_ITEM_SIZE, LEGEND_ITEM_SIZE, Colors.GREEN);
    Raylib.DrawText("Start", legendX + LEGEND_ITEM_SIZE + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, LEGEND_ITEM_SIZE, LEGEND_ITEM_SIZE, Colors.RED);
    Raylib.DrawText("Goal", legendX + LEGEND_ITEM_SIZE + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, LEGEND_ITEM_SIZE, LEGEND_ITEM_SIZE, Colors.BLUE);
    Raylib.DrawText("Path", legendX + LEGEND_ITEM_SIZE + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, LEGEND_ITEM_SIZE, LEGEND_ITEM_SIZE, Colors.DARKGRAY);
    Raylib.DrawText("Wall", legendX + LEGEND_ITEM_SIZE + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, LEGEND_ITEM_SIZE, LEGEND_ITEM_SIZE, Colors.LIGHTGRAY);
    Raylib.DrawText("Walkable", legendX + LEGEND_ITEM_SIZE + 5, currentY, 16, Colors.WHITE);
  }

  public static int getHeight() {
    return 6 * 30;
  }
}
