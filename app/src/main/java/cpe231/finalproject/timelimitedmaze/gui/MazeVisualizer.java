package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.DropdownComponent;
import cpe231.finalproject.timelimitedmaze.gui.components.LegendPanel;
import cpe231.finalproject.timelimitedmaze.gui.components.LogPanel;
import cpe231.finalproject.timelimitedmaze.gui.components.MazeRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.PathRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.StartGoalRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.StatisticsPanel;
import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import com.raylib.Raylib;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MazeVisualizer {
  private Maze maze;
  private SolverResult result;
  @SuppressWarnings("unused")
  private String algorithmName;
  private Set<Coordinate> pathSet;
  private int cellSize;
  private List<String> logLines = List.of();
  private int logScrollOffset = 0;
  private boolean logFollowTail = true;
  private int lastLogCount = 0;
  private String solverName;

  public MazeVisualizer(Maze maze, SolverResult result, String algorithmName) {
    this.maze = maze;
    this.result = result;
    this.algorithmName = algorithmName;
    this.pathSet = result != null ? new HashSet<>(result.path()) : new HashSet<>();
    this.solverName = algorithmName;
    calculateCellSize();
  }

  public void updateResult(SolverResult result, String algorithmName) {
    this.result = result;
    this.algorithmName = algorithmName;
    this.solverName = algorithmName;
    this.pathSet = result != null ? new HashSet<>(result.path()) : new HashSet<>();
  }

  public void updateMaze(Maze newMaze) {
    this.maze = newMaze;
    this.result = null;
    this.pathSet = new HashSet<>();
    calculateCellSize();
  }

  public void initializeTexture() {
    // No texture initialization required for log panel.
  }

  private void calculateCellSize() {
    if (maze == null) {
      return;
    }
    int availableWidth = GUIConstants.MAZE_PANEL_WIDTH - 2 * GUIConstants.PADDING;
    int availableHeight = GUIConstants.MAZE_PANEL_HEIGHT - 2 * GUIConstants.PADDING;
    int cellSizeByWidth = availableWidth / maze.getWidth();
    int cellSizeByHeight = availableHeight / maze.getHeight();
    this.cellSize = Math.min(cellSizeByWidth, cellSizeByHeight);
    this.cellSize = Math.max(1, this.cellSize);
  }

  public void render() {
    renderMaze();
    renderPath();
    renderStartAndGoal();
    renderStatistics(List.of(), null, false, null, null, null, false, List.of(), Map.of(), false, 0.0f,
        com.raylib.Helpers.newVector2(0, 0));
  }

  public void render(List<MazeSolver> availableSolvers, Integer selectedSolverIndex, boolean dropdownOpen,
      SolverResult currentResult, String errorMessage, String selectedMazeFileName,
      boolean mazeDropdownOpen, List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap,
      List<String> logs, boolean solvingInProgress, float mouseWheelDelta, Raylib.Vector2 mousePos,
      String solverName) {
    this.logLines = logs != null ? logs : List.of();
    this.solverName = solverName;
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
        selectedMazeFileName, mazeDropdownOpen, availableMazeFiles, mazeValidityMap, solvingInProgress,
        mouseWheelDelta, mousePos);
  }

  private void renderMaze() {
    MazeRenderer.render(maze, cellSize, pathSet);
  }

  private void renderPath() {
    if (result == null) {
      return;
    }
    PathRenderer.render(result.path(), cellSize);
  }

  private void renderStartAndGoal() {
    StartGoalRenderer.render(maze, cellSize);
  }

  private void renderStatistics(List<MazeSolver> availableSolvers, Integer selectedSolverIndex, boolean dropdownOpen,
      SolverResult currentResult, String errorMessage, String selectedMazeFileName,
      boolean mazeDropdownOpen, List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap,
      boolean solvingInProgress, float mouseWheelDelta, Raylib.Vector2 mousePos) {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int panelY = GUIConstants.PADDING;
    int currentY = panelY;

    StatisticsPanel.renderBackground(panelX);
    currentY = StatisticsPanel.renderTitle(panelX, currentY, "Solver Statistics");

    int algorithmDropdownY = currentY;
    currentY += GUIConstants.DROPDOWN_HEIGHT + 10;

    int mazeDropdownY = currentY;
    currentY += GUIConstants.DROPDOWN_HEIGHT + 10;

    currentY = StatisticsPanel.renderErrorMessage(panelX, currentY, errorMessage);
    currentY = StatisticsPanel.renderMazeInfo(panelX, currentY, maze);

    SolverResult displayResult = currentResult != null ? currentResult : result;
    if (displayResult != null) {
      long executionTimeNs = displayResult.endTimeNs() - displayResult.startTimeNs();
      double executionTimeMs = executionTimeNs / 1_000_000.0;
      currentY = StatisticsPanel.renderSolverStatistics(panelX, currentY, displayResult.path().size(),
          displayResult.totalCost(), executionTimeMs);
    } else {
      currentY += StatisticsPanel.LINE_HEIGHT + 20;
    }

    LegendPanel.render(panelX, currentY);
    currentY += LegendPanel.getHeight() + 10;

    int logPanelX = panelX + 10;
    int logPanelY = currentY;
    int logPanelWidth = GUIConstants.STATS_PANEL_WIDTH - 20;
    int logPanelHeight = GUIConstants.WINDOW_HEIGHT - logPanelY - GUIConstants.PADDING;
    if (logPanelHeight < LogPanel.MIN_HEIGHT) {
      logPanelHeight = LogPanel.MIN_HEIGHT;
    }
    updateLogScroll(mouseWheelDelta, mousePos, logPanelX, logPanelY, logPanelWidth, logPanelHeight);
    LogPanel.render(logPanelX, logPanelY, logPanelWidth, logPanelHeight, logLines, logScrollOffset,
        solvingInProgress, solverName, currentResult != null || result != null);

    DropdownComponent<MazeSolver> algorithmDropdown = new DropdownComponent<>(panelX + 10, algorithmDropdownY,
        solver -> solver.getAlgorithmName());
    DropdownComponent<String> mazeDropdown = new DropdownComponent<>(panelX + 10, mazeDropdownY, fileName -> fileName);

    boolean isAlgorithmOpen = dropdownOpen && !mazeDropdownOpen;
    boolean isMazeOpen = mazeDropdownOpen;

    if (isAlgorithmOpen) {
      mazeDropdown.renderWithValidityMap(availableMazeFiles, selectedMazeFileName, false, "Select Maze...", mazeValidityMap);
      algorithmDropdown.render(availableSolvers, selectedSolverIndex != null ? availableSolvers.get(selectedSolverIndex) : null,
          true, "Select Algorithm...");
    } else if (isMazeOpen) {
      algorithmDropdown.render(availableSolvers, selectedSolverIndex != null ? availableSolvers.get(selectedSolverIndex) : null,
          false, "Select Algorithm...");
      mazeDropdown.renderWithValidityMap(availableMazeFiles, selectedMazeFileName, true, "Select Maze...", mazeValidityMap);
    } else {
      algorithmDropdown.render(availableSolvers, selectedSolverIndex != null ? availableSolvers.get(selectedSolverIndex) : null,
          false, "Select Algorithm...");
      mazeDropdown.renderWithValidityMap(availableMazeFiles, selectedMazeFileName, false, "Select Maze...", mazeValidityMap);
    }
  }

  public void resetLogScroll() {
    logScrollOffset = 0;
    logFollowTail = true;
    lastLogCount = logLines.size();
  }

  private void updateLogScroll(float mouseWheelDelta, Raylib.Vector2 mousePos, int x, int y, int width, int height) {
    int maxVisible = Math.max(1, height / LogPanel.LINE_HEIGHT);
    int maxOffset = Math.max(0, logLines.size() - maxVisible);

    Raylib.Rectangle panelRect = com.raylib.Helpers.newRectangle(x, y, width, height);
    boolean mouseOverPanel = Raylib.CheckCollisionPointRec(mousePos, panelRect);

    if (mouseWheelDelta != 0 && mouseOverPanel) {
      int deltaLines = (int) (-mouseWheelDelta * 3);
      logScrollOffset = clamp(logScrollOffset + deltaLines, 0, maxOffset);
      logFollowTail = logScrollOffset == maxOffset;
    } else {
      if (logFollowTail) {
        logScrollOffset = maxOffset;
      } else if (logScrollOffset > maxOffset) {
        logScrollOffset = maxOffset;
      }
    }

    if (logLines.size() > lastLogCount && logFollowTail) {
      logScrollOffset = maxOffset;
    }

    lastLogCount = logLines.size();
  }

  private int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  public Integer checkDropdownClick(Raylib.Vector2 mousePos, boolean isOpen, List<MazeSolver> availableSolvers) {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int dropdownY = GUIConstants.PADDING + 30 + 10;

    DropdownComponent<MazeSolver> dropdown = new DropdownComponent<>(dropdownX, dropdownY,
        solver -> solver.getAlgorithmName());
    return dropdown.checkClick(mousePos, availableSolvers, isOpen);
  }

  public Integer checkMazeDropdownClick(Raylib.Vector2 mousePos, boolean isOpen,
      List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int algorithmDropdownY = GUIConstants.PADDING + 30 + 10;
    int dropdownY = algorithmDropdownY + GUIConstants.DROPDOWN_HEIGHT + 10;

    DropdownComponent<String> dropdown = new DropdownComponent<>(dropdownX, dropdownY, fileName -> fileName);
    return dropdown.checkClickWithValidityMap(mousePos, availableMazeFiles, isOpen, mazeValidityMap);
  }

  public int getWindowWidth() {
    return GUIConstants.WINDOW_WIDTH;
  }

  public int getWindowHeight() {
    return GUIConstants.WINDOW_HEIGHT;
  }
}
