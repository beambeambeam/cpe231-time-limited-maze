package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.DropdownComponent;
import cpe231.finalproject.timelimitedmaze.gui.components.LegendPanel;
import cpe231.finalproject.timelimitedmaze.gui.components.MazeRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.PathRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.StartGoalRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.StatisticsPanel;
import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.gui.utils.TextureManager;
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
  private final TextureManager textureManager;
  private int cellSize;

  public MazeVisualizer(Maze maze, SolverResult result, String algorithmName) {
    this.maze = maze;
    this.result = result;
    this.algorithmName = algorithmName;
    this.pathSet = result != null ? new HashSet<>(result.path()) : new HashSet<>();
    this.textureManager = new TextureManager();
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
    textureManager.initialize();
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
      boolean mazeDropdownOpen, List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
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

    textureManager.render(panelX, currentY, 280, 180);

    DropdownComponent<MazeSolver> algorithmDropdown = new DropdownComponent<>(panelX + 10, algorithmDropdownY,
        solver -> solver.getAlgorithmName());
    algorithmDropdown.render(availableSolvers, selectedSolverIndex != null ? availableSolvers.get(selectedSolverIndex) : null,
        dropdownOpen, "Select Algorithm...");

    DropdownComponent<String> mazeDropdown = new DropdownComponent<>(panelX + 10, mazeDropdownY, fileName -> fileName);
    mazeDropdown.renderWithValidityMap(availableMazeFiles, selectedMazeFileName, mazeDropdownOpen,
        "Select Maze...", mazeValidityMap);
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
