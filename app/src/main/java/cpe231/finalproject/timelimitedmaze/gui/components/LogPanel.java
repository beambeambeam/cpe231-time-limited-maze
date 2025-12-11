package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import com.raylib.Colors;
import com.raylib.Raylib;
import java.util.List;

public final class LogPanel {
  private static final int PADDING = 8;
  private static final int FONT_SIZE = 16;
  private static final int TITLE_HEIGHT = FONT_SIZE + PADDING;
  public static final int LINE_HEIGHT = 18;
  public static final int MIN_HEIGHT = 120;

  private LogPanel() {
  }

  public static void render(int x, int y, int width, int height, List<String> logs, int scrollOffset,
      boolean solvingInProgress, String solverName, boolean hasRun) {
    Raylib.DrawRectangle(x, y, width, height, GUIConstants.DROPDOWN_BACKGROUND_COLOR);
    Raylib.DrawRectangleLines(x, y, width, height, GUIConstants.DROPDOWN_BORDER_COLOR);

    StringBuilder title = new StringBuilder("Logs");
    if (solverName != null && !solverName.isBlank()) {
      title.append(" - ").append(solverName);
    }
    if (solvingInProgress) {
      title.append(" (running)");
    }
    Raylib.DrawText(title.toString(), x + PADDING, y + PADDING, FONT_SIZE, Colors.WHITE);

    int contentY = y + TITLE_HEIGHT + PADDING;
    int availableHeight = height - (TITLE_HEIGHT + 2 * PADDING);
    if (availableHeight < LINE_HEIGHT) {
      availableHeight = LINE_HEIGHT;
    }

    int maxVisible = Math.max(1, availableHeight / LINE_HEIGHT);
    int maxOffset = Math.max(0, logs.size() - maxVisible);
    int startIndex = clamp(scrollOffset, 0, maxOffset);
    int endIndex = Math.min(logs.size(), startIndex + maxVisible);

    if (logs.isEmpty()) {
      String placeholder;
      if (solvingInProgress) {
        placeholder = "Solving... awaiting logs.";
      } else if (solverName != null) {
        placeholder = hasRun ? "No logs produced." : "Select solve to view logs.";
      } else {
        placeholder = "Select an algorithm to view logs.";
      }
      Raylib.DrawText(placeholder, x + PADDING, contentY, FONT_SIZE, Colors.LIGHTGRAY);
      return;
    }

    for (int i = startIndex; i < endIndex; i++) {
      String line = logs.get(i);
      Raylib.DrawText(line, x + PADDING, contentY, FONT_SIZE, Colors.LIGHTGRAY);
      contentY += LINE_HEIGHT;
    }
  }

  private static int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }
}
