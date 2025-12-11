package cpe231.finalproject.timelimitedmaze.gui.components;

import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import com.raylib.Colors;
import com.raylib.Raylib;
import java.util.ArrayList;
import java.util.List;

public final class LogPanel {
  public static final int PADDING = 8;
  private static final int FONT_SIZE = 16;
  private static final int TITLE_HEIGHT = FONT_SIZE + PADDING;
  public static final int LINE_HEIGHT = 18;
  public static final int MIN_HEIGHT = 120;
  private static final String TIMESTAMP_SEPARATOR = " ";
  private static final String TIMESTAMP_PLACEHOLDER = "HH:mm:ss.SSS";
  private static final int TIMESTAMP_COLUMN_WIDTH;

  static {
    int timestampWidth = Raylib.MeasureText(TIMESTAMP_PLACEHOLDER, FONT_SIZE);
    TIMESTAMP_COLUMN_WIDTH = timestampWidth + 8;
  }

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

    List<LogLine> wrappedLines = wrapLogLines(logs, width - 2 * PADDING);
    int maxVisible = Math.max(1, availableHeight / LINE_HEIGHT);
    int maxOffset = Math.max(0, wrappedLines.size() - maxVisible);
    int startIndex = clamp(scrollOffset, 0, maxOffset);
    int endIndex = Math.min(wrappedLines.size(), startIndex + maxVisible);

    for (int i = startIndex; i < endIndex && contentY + LINE_HEIGHT <= y + height; i++) {
      LogLine logLine = wrappedLines.get(i);
      if (logLine.isContinuation) {
        Raylib.DrawText(logLine.message, x + PADDING + TIMESTAMP_COLUMN_WIDTH, contentY, FONT_SIZE, Colors.LIGHTGRAY);
      } else {
        Raylib.DrawText(logLine.timestamp, x + PADDING, contentY, FONT_SIZE, Colors.LIGHTGRAY);
        Raylib.DrawText(logLine.message, x + PADDING + TIMESTAMP_COLUMN_WIDTH, contentY, FONT_SIZE, Colors.LIGHTGRAY);
      }
      contentY += LINE_HEIGHT;
    }
  }

  private static List<LogLine> wrapLogLines(List<String> logs, int maxWidth) {
    List<LogLine> wrappedLines = new ArrayList<>();
    int messageMaxWidth = maxWidth - TIMESTAMP_COLUMN_WIDTH;

    for (String log : logs) {
      int separatorIndex = log.indexOf(TIMESTAMP_SEPARATOR);
      if (separatorIndex < 0) {
        wrappedLines.add(new LogLine("", log, false));
        continue;
      }

      String timestamp = log.substring(0, separatorIndex);
      String message = log.substring(separatorIndex + TIMESTAMP_SEPARATOR.length());
      List<String> messageLines = wrapText(message, messageMaxWidth);

      for (int i = 0; i < messageLines.size(); i++) {
        wrappedLines.add(new LogLine(i == 0 ? timestamp : "", messageLines.get(i), i > 0));
      }
    }

    return wrappedLines;
  }

  private static List<String> wrapText(String text, int maxWidth) {
    List<String> lines = new ArrayList<>();
    if (text.isEmpty()) {
      lines.add("");
      return lines;
    }

    String[] words = text.split(" ", -1);
    StringBuilder currentLine = new StringBuilder();

    for (String word : words) {
      String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
      int testWidth = Raylib.MeasureText(testLine, FONT_SIZE);

      if (testWidth <= maxWidth) {
        currentLine = new StringBuilder(testLine);
      } else {
        if (currentLine.length() > 0) {
          lines.add(currentLine.toString());
          currentLine = new StringBuilder(word);
        } else {
          lines.add(word);
        }
      }
    }

    if (currentLine.length() > 0) {
      lines.add(currentLine.toString());
    }

    return lines.isEmpty() ? List.of("") : lines;
  }

  public static int calculateWrappedLineCount(List<String> logs, int maxWidth) {
    int messageMaxWidth = maxWidth - TIMESTAMP_COLUMN_WIDTH;
    int totalLines = 0;

    for (String log : logs) {
      int separatorIndex = log.indexOf(TIMESTAMP_SEPARATOR);
      if (separatorIndex < 0) {
        totalLines += 1;
        continue;
      }

      String message = log.substring(separatorIndex + TIMESTAMP_SEPARATOR.length());
      List<String> messageLines = wrapText(message, messageMaxWidth);
      totalLines += messageLines.size();
    }

    return totalLines;
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

  private static class LogLine {
    final String timestamp;
    final String message;
    final boolean isContinuation;

    LogLine(String timestamp, String message, boolean isContinuation) {
      this.timestamp = timestamp;
      this.message = message;
      this.isContinuation = isContinuation;
    }
  }
}
