package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.components.ButtonComponent;
import cpe231.finalproject.timelimitedmaze.gui.components.DropdownComponent;
import cpe231.finalproject.timelimitedmaze.gui.components.LegendPanel;
import cpe231.finalproject.timelimitedmaze.gui.components.StartGoalRenderer;
import cpe231.finalproject.timelimitedmaze.gui.components.StatisticsPanel;
import cpe231.finalproject.timelimitedmaze.gui.state.GAAnimationState;
import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import com.raylib.Helpers;
import com.raylib.Raylib;
import java.util.List;

public final class GAAnimationView {
  private final DropdownComponent<String> mazeDropdown;
  private final ButtonComponent playStopButton;

  public GAAnimationView() {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int mazeDropdownY = GUIConstants.PADDING + 30 + 10;
    this.mazeDropdown = new DropdownComponent<>(dropdownX, mazeDropdownY, fileName -> fileName);
    this.playStopButton = new ButtonComponent(panelX + 10,
        GUIConstants.WINDOW_HEIGHT - GUIConstants.BUTTON_HEIGHT - 20);
  }

  public void render(GAAnimationState state) {
    renderMaze(state);
    renderPath(state);
    renderStartAndGoal(state);
    renderStatistics(state);
    renderControls(state);
    renderDropdowns(state);
  }

  private void renderMaze(GAAnimationState state) {
    GAMazeRenderer.render(state.maze(), state.cellSize(), state.currentBestPath());
  }

  private void renderPath(GAAnimationState state) {
    GAPathRenderer.render(state.currentBestPath(), state.allPopulationPaths(), state.cellSize());
  }

  private void renderStartAndGoal(GAAnimationState state) {
    StartGoalRenderer.render(state.maze(), state.cellSize());
  }

  private void renderStatistics(GAAnimationState state) {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int panelY = GUIConstants.PADDING;
    int currentY = panelY;

    StatisticsPanel.renderBackground(panelX);
    currentY = StatisticsPanel.renderTitle(panelX, currentY, "GA Statistics");
    currentY = StatisticsPanel.renderMazeInfo(panelX, currentY, state.maze());

    if (state.maze() != null) {
      currentY = StatisticsPanel.renderGAStatistics(panelX, currentY, state.currentGeneration(),
          state.currentBestFitness(), state.currentBestPath().size(), state.goalReached());
    }

    LegendPanel.render(panelX, currentY);
  }

  private void renderControls(GAAnimationState state) {
    String buttonText = state.isPlaying() ? "Stop" : "Play";
    Raylib.Color activeColor = Helpers.newColor((byte) 200, (byte) 50, (byte) 50, (byte) 255);
    Raylib.Color inactiveColor = Helpers.newColor((byte) 50, (byte) 200, (byte) 50, (byte) 255);
    playStopButton.render(buttonText, state.isPlaying(), activeColor, inactiveColor);
  }

  private void renderDropdowns(GAAnimationState state) {
    int panelX = GUIConstants.MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int mazeDropdownY = GUIConstants.PADDING + 30 + 10;

    int maxMazeOptionsHeight = calculateMaxMazeOptionsHeight(state.availableMazeFiles(), state.mazeValidityMap());
    int speedDropdownY = mazeDropdownY + GUIConstants.DROPDOWN_HEIGHT + maxMazeOptionsHeight + 10;

    DropdownComponent<Integer> speedDropdownWithY = new DropdownComponent<>(dropdownX, speedDropdownY,
        speed -> speed + " ms/gen");

    mazeDropdown.renderWithValidityMap(state.availableMazeFiles(),
        state.selectedMazeFileName() != null ? state.selectedMazeFileName() : null,
        state.mazeDropdownOpen(), "Select Maze...", state.mazeValidityMap());

    List<Integer> speedOptions = List.of(100, 500, 1000);
    Integer selectedSpeed = state.generationSpeedMs();
    speedDropdownWithY.render(speedOptions, selectedSpeed, state.speedDropdownOpen(), "Select Speed...");
  }

  private int calculateMaxMazeOptionsHeight(List<String> availableMazeFiles,
      java.util.Map<String, Boolean> mazeValidityMap) {
    int validCount = 0;
    for (String fileName : availableMazeFiles) {
      if (mazeValidityMap.getOrDefault(fileName, false)) {
        validCount++;
      }
    }
    return validCount * GUIConstants.DROPDOWN_OPTION_HEIGHT;
  }
}
