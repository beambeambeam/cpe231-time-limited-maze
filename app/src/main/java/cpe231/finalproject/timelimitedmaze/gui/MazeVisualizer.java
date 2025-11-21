package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import cpe231.finalproject.timelimitedmaze.utils.MazeCellType;
import com.raylib.Raylib;
import com.raylib.Colors;
import com.raylib.Helpers;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MazeVisualizer {
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 800;
  private static final int STATS_PANEL_WIDTH = 300;
  private static final int MAZE_PANEL_WIDTH = WINDOW_WIDTH - STATS_PANEL_WIDTH;
  private static final int MAZE_PANEL_HEIGHT = WINDOW_HEIGHT;
  private static final int PADDING = 20;
  private static final int DROPDOWN_WIDTH = 250;
  private static final int DROPDOWN_HEIGHT = 30;
  private static final int DROPDOWN_OPTION_HEIGHT = 30;

  private Maze maze;
  private SolverResult result;
  @SuppressWarnings("unused")
  private String algorithmName;
  private Set<Coordinate> pathSet;
  private Raylib.Texture chiikawaTexture;
  private boolean textureLoaded = false;
  private int cellSize;

  public MazeVisualizer(Maze maze, SolverResult result, String algorithmName) {
    this.maze = maze;
    this.result = result;
    this.algorithmName = algorithmName;
    this.pathSet = result != null ? new HashSet<>(result.path()) : new HashSet<>();
    calculateCellSize();
  }

  public void updateResult(SolverResult result, String algorithmName) {
    this.result = result;
    this.algorithmName = algorithmName;
    this.pathSet = result != null ? new HashSet<>(result.path()) : new HashSet<>();
  }

  public void updateMaze(Maze newMaze) {
    this.maze = newMaze;
    this.result = null;
    this.pathSet = new HashSet<>();
    calculateCellSize();
  }

  public void initializeTexture() {
    if (!textureLoaded) {
      chiikawaTexture = loadImage();
      textureLoaded = true;
    }
  }

  private void ensureTextureLoaded() {
    if (!textureLoaded && chiikawaTexture == null) {
      chiikawaTexture = loadImage();
      textureLoaded = true;
    }
  }

  private Raylib.Texture loadImage() {
    try {
      InputStream imageStream = getClass().getClassLoader().getResourceAsStream("ascii-art.png");
      if (imageStream == null) {
        System.err.println("Warning: Could not find ascii-art.png in resources");
        return null;
      }

      String tempPath = System.getProperty("java.io.tmpdir") + "/chiikawa_temp_" + System.currentTimeMillis() + ".jpg";
      java.nio.file.Files.copy(imageStream, java.nio.file.Paths.get(tempPath),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      imageStream.close();

      Raylib.Image image = Raylib.LoadImage(tempPath);
      if (image == null) {
        System.err.println("Warning: Failed to load image from " + tempPath);
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempPath));
        return null;
      }

      Raylib.Texture texture = Raylib.LoadTextureFromImage(image);
      Raylib.UnloadImage(image);

      java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempPath));

      return texture;
    } catch (Exception e) {
      System.err.println("Error loading ascii-art.png: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
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
    renderStatistics(List.of(), null, false, null, null, null, false, List.of(), Map.of());
  }

  public void render(List<MazeSolver> availableSolvers, Integer selectedSolverIndex, boolean dropdownOpen,
      SolverResult currentResult, String errorMessage, String selectedMazeFileName,
      boolean mazeDropdownOpen, List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    if (currentResult != null && currentResult != result) {
      this.result = currentResult;
      this.pathSet = new HashSet<>(currentResult.path());
    } else if (currentResult == null && result != null) {
      this.result = null;
      this.pathSet = new HashSet<>();
    }
    renderMaze();
    renderPath();
    renderStartAndGoal();
    renderStatistics(availableSolvers, selectedSolverIndex, dropdownOpen, currentResult, errorMessage,
        selectedMazeFileName, mazeDropdownOpen, availableMazeFiles, mazeValidityMap);
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
    if (result == null) {
      return;
    }

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

  private void renderStatistics(List<MazeSolver> availableSolvers, Integer selectedSolverIndex, boolean dropdownOpen,
      SolverResult currentResult, String errorMessage, String selectedMazeFileName,
      boolean mazeDropdownOpen, List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    int panelX = MAZE_PANEL_WIDTH;
    int panelY = PADDING;
    int lineHeight = 30;
    int currentY = panelY;

    Raylib.DrawRectangle(panelX, 0, STATS_PANEL_WIDTH, WINDOW_HEIGHT,
        Helpers.newColor((byte) 240, (byte) 240, (byte) 240, (byte) 255));

    String title = "Solver Statistics";
    Raylib.DrawText(title, panelX + 10, currentY, 24, Colors.BLACK);
    currentY += lineHeight + 10;

    int algorithmDropdownY = currentY;
    currentY += DROPDOWN_HEIGHT + 10;

    int mazeDropdownY = currentY;
    currentY += DROPDOWN_HEIGHT + 10;

    if (errorMessage != null) {
      Raylib.DrawText(errorMessage, panelX + 10, currentY, 16, Colors.RED);
      currentY += lineHeight + 10;
    }

    String mazeName = "Maze: " + maze.getName();
    Raylib.DrawText(mazeName, panelX + 10, currentY, 18, Colors.DARKGRAY);
    currentY += lineHeight;

    String dimensions = String.format("Size: %d x %d", maze.getWidth(), maze.getHeight());
    Raylib.DrawText(dimensions, panelX + 10, currentY, 18, Colors.DARKGRAY);
    currentY += lineHeight + 10;

    SolverResult displayResult = currentResult != null ? currentResult : result;
    if (displayResult != null) {
      String pathLength = "Path Length: " + displayResult.path().size();
      Raylib.DrawText(pathLength, panelX + 10, currentY, 20, Colors.BLUE);
      currentY += lineHeight;

      String totalCost = "Total Cost: " + displayResult.totalCost();
      Raylib.DrawText(totalCost, panelX + 10, currentY, 20, Colors.BLUE);
      currentY += lineHeight;

      long executionTimeNs = displayResult.endTimeNs() - displayResult.startTimeNs();
      double executionTimeMs = executionTimeNs / 1_000_000.0;
      String executionTime = String.format("Execution Time: %.3f ms", executionTimeMs);
      Raylib.DrawText(executionTime, panelX + 10, currentY, 20, Colors.BLUE);
      currentY += lineHeight + 20;
    } else {
      currentY += lineHeight + 20;
    }

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
    currentY += lineHeight + 10;

    ensureTextureLoaded();
    if (chiikawaTexture != null) {
      int maxImageWidth = 280;
      int maxImageHeight = 180;

      int textureWidth = chiikawaTexture.width();
      int textureHeight = chiikawaTexture.height();

      float scaleX = (float) maxImageWidth / textureWidth;
      float scaleY = (float) maxImageHeight / textureHeight;
      float scale = Math.min(scaleX, scaleY);

      int imageWidth = (int) (textureWidth * scale);
      int imageX = panelX + (STATS_PANEL_WIDTH - imageWidth) / 2;

      Raylib.Vector2 position = Helpers.newVector2(imageX, currentY);
      Raylib.DrawTextureEx(chiikawaTexture, position, 0.0f, scale, Colors.WHITE);
    }

    renderMazeDropdown(panelX + 10, mazeDropdownY, selectedMazeFileName, mazeDropdownOpen,
        availableMazeFiles, mazeValidityMap);
    renderDropdown(panelX + 10, algorithmDropdownY, availableSolvers, selectedSolverIndex, dropdownOpen, mazeDropdownY);
  }

  private void renderDropdown(int x, int y, List<MazeSolver> availableSolvers, Integer selectedSolverIndex,
      boolean isOpen, int mazeDropdownY) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    String displayText = selectedSolverIndex == null
        ? "Select Algorithm..."
        : availableSolvers.get(selectedSolverIndex).getAlgorithmName();

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    Raylib.DrawRectangle((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(), Colors.WHITE);
    Raylib.DrawRectangleLines((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(), Colors.DARKGRAY);

    Raylib.DrawText(displayText, x + 5, y + 6, 16, Colors.BLACK);

    int arrowX = x + DROPDOWN_WIDTH - 20;
    int arrowY = y + DROPDOWN_HEIGHT / 2;
    if (isOpen) {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY - 5),
          Helpers.newVector2(arrowX + 10, arrowY - 5),
          Helpers.newVector2(arrowX + 5, arrowY + 5), Colors.DARKGRAY);
    } else {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY + 5),
          Helpers.newVector2(arrowX + 10, arrowY + 5),
          Helpers.newVector2(arrowX + 5, arrowY - 5), Colors.DARKGRAY);
    }

    if (isOpen) {
      int totalOptionsHeight = availableSolvers.size() * DROPDOWN_OPTION_HEIGHT;
      int optionsStartY = y + DROPDOWN_HEIGHT;

      Raylib.DrawRectangle(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight, Colors.WHITE);
      Raylib.DrawRectangleLines(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight, Colors.DARKGRAY);

      for (int i = 0; i < availableSolvers.size(); i++) {
        int optionY = optionsStartY + i * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        boolean isHovered = Raylib.CheckCollisionPointRec(mousePos, optionRect);

        if (isHovered) {
          Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
              (int) optionRect.width(), (int) optionRect.height(),
              Helpers.newColor((byte) 220, (byte) 220, (byte) 220, (byte) 255));
        }

        String optionText = availableSolvers.get(i).getAlgorithmName();
        Raylib.DrawText(optionText, x + 5, optionY + 6, 16, Colors.BLACK);
      }
    }
  }

  public Integer checkDropdownClick(Raylib.Vector2 mousePos, boolean isOpen, List<MazeSolver> availableSolvers) {
    int panelX = MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int dropdownY = PADDING + 30 + 10;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(dropdownX, dropdownY, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    if (Raylib.CheckCollisionPointRec(mousePos, dropdownRect)) {
      return -1;
    }

    if (isOpen) {
      for (int i = 0; i < availableSolvers.size(); i++) {
        int optionY = dropdownY + DROPDOWN_HEIGHT + i * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(dropdownX, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        if (Raylib.CheckCollisionPointRec(mousePos, optionRect)) {
          return i;
        }
      }
    }

    return null;
  }

  public Integer checkMazeDropdownClick(Raylib.Vector2 mousePos, boolean isOpen,
      List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    int panelX = MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int algorithmDropdownY = PADDING + 30 + 10;
    int dropdownY = algorithmDropdownY + DROPDOWN_HEIGHT + 10;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(dropdownX, dropdownY, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    if (Raylib.CheckCollisionPointRec(mousePos, dropdownRect)) {
      return -1;
    }

    if (isOpen) {
      for (int i = 0; i < availableMazeFiles.size(); i++) {
        String fileName = availableMazeFiles.get(i);
        if (!mazeValidityMap.getOrDefault(fileName, false)) {
          continue;
        }
        int optionY = dropdownY + DROPDOWN_HEIGHT + i * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(dropdownX, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        if (Raylib.CheckCollisionPointRec(mousePos, optionRect)) {
          return i;
        }
      }
    }

    return null;
  }

  private void renderMazeDropdown(int x, int y, String selectedMazeFileName, boolean isOpen,
      List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    String displayText = selectedMazeFileName == null
        ? "Select Maze..."
        : selectedMazeFileName;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    Raylib.DrawRectangle((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(), Colors.WHITE);
    Raylib.DrawRectangleLines((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(), Colors.DARKGRAY);

    Raylib.DrawText(displayText, x + 5, y + 6, 16, Colors.BLACK);

    int arrowX = x + DROPDOWN_WIDTH - 20;
    int arrowY = y + DROPDOWN_HEIGHT / 2;
    if (isOpen) {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY - 5),
          Helpers.newVector2(arrowX + 10, arrowY - 5),
          Helpers.newVector2(arrowX + 5, arrowY + 5), Colors.DARKGRAY);
    } else {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY + 5),
          Helpers.newVector2(arrowX + 10, arrowY + 5),
          Helpers.newVector2(arrowX + 5, arrowY - 5), Colors.DARKGRAY);
    }

    if (isOpen) {
      int totalOptionsHeight = availableMazeFiles.size() * DROPDOWN_OPTION_HEIGHT;
      int optionsStartY = y + DROPDOWN_HEIGHT;

      Raylib.DrawRectangle(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight, Colors.WHITE);
      Raylib.DrawRectangleLines(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight, Colors.DARKGRAY);

      for (int i = 0; i < availableMazeFiles.size(); i++) {
        String fileName = availableMazeFiles.get(i);
        boolean isValid = mazeValidityMap.getOrDefault(fileName, false);
        int optionY = optionsStartY + i * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        if (isValid) {
          boolean isHovered = Raylib.CheckCollisionPointRec(mousePos, optionRect);

          if (isHovered) {
            Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
                (int) optionRect.width(), (int) optionRect.height(),
                Helpers.newColor((byte) 220, (byte) 220, (byte) 220, (byte) 255));
          }

          Raylib.DrawText(fileName, x + 5, optionY + 6, 16, Colors.BLACK);
        } else {
          Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
              (int) optionRect.width(), (int) optionRect.height(),
              Helpers.newColor((byte) 240, (byte) 240, (byte) 240, (byte) 255));
          Raylib.DrawText(fileName, x + 5, optionY + 6, 16, Colors.GRAY);
        }
      }
    }
  }

  public int getWindowWidth() {
    return WINDOW_WIDTH;
  }

  public int getWindowHeight() {
    return WINDOW_HEIGHT;
  }
}
