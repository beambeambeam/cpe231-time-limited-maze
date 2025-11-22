package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.gui.state.GAAnimationState;
import cpe231.finalproject.timelimitedmaze.gui.utils.GUIConstants;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeValidator;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GAAnimationController {
  private GAAnimationState state;
  private final GAAnimationView view;
  private final GAInputHandler inputHandler;
  private GARunner gaRunner;

  public GAAnimationController() {
    List<String> availableMazeFiles = MazeFileLister.listMazeFiles();
    Map<String, Boolean> mazeValidityMap = new HashMap<>();
    for (String fileName : availableMazeFiles) {
      mazeValidityMap.put(fileName, MazeValidator.isValidMaze(fileName));
    }

    this.state = new GAAnimationState(
        null,
        false,
        0,
        new ArrayList<>(),
        Double.NEGATIVE_INFINITY,
        false,
        null,
        false,
        availableMazeFiles,
        mazeValidityMap,
        500,
        false,
        0,
        0,
        new ArrayList<>());

    this.view = new GAAnimationView();
    this.inputHandler = new GAInputHandler();
    this.gaRunner = null;
  }

  public void handleInput() {
    GAInputHandler.InputResult result = inputHandler.handleInput(state);

    switch (result.type()) {
      case TOGGLE_MAZE_DROPDOWN:
        state = new GAAnimationState(
            state.maze(), false, state.currentGeneration(), state.currentBestPath(),
            state.currentBestFitness(), state.goalReached(), state.selectedMazeFileName(), true,
            state.availableMazeFiles(), state.mazeValidityMap(), state.generationSpeedMs(), false,
            state.lastGenerationTime(), state.cellSize(), state.allPopulationPaths());
        break;
      case SELECT_MAZE:
        if (result.data() instanceof String fileName) {
          loadMaze(fileName);
        }
        break;
      case TOGGLE_SPEED_DROPDOWN:
        state = new GAAnimationState(
            state.maze(), state.isPlaying(), state.currentGeneration(), state.currentBestPath(),
            state.currentBestFitness(), state.goalReached(), state.selectedMazeFileName(), false,
            state.availableMazeFiles(), state.mazeValidityMap(), state.generationSpeedMs(), true,
            state.lastGenerationTime(), state.cellSize(), state.allPopulationPaths());
        break;
      case SELECT_SPEED:
        if (result.data() instanceof Integer speed) {
          state = new GAAnimationState(
              state.maze(), state.isPlaying(), state.currentGeneration(), state.currentBestPath(),
              state.currentBestFitness(), state.goalReached(), state.selectedMazeFileName(), false,
              state.availableMazeFiles(), state.mazeValidityMap(), speed, false,
              state.lastGenerationTime(), state.cellSize(), state.allPopulationPaths());
        }
        break;
      case TOGGLE_PLAY_STOP:
        if (state.maze() != null) {
          boolean newIsPlaying = !state.isPlaying();
          long newLastGenerationTime = newIsPlaying ? System.currentTimeMillis() : state.lastGenerationTime();
          state = new GAAnimationState(
              state.maze(), newIsPlaying, state.currentGeneration(), state.currentBestPath(),
              state.currentBestFitness(), state.goalReached(), state.selectedMazeFileName(), false,
              state.availableMazeFiles(), state.mazeValidityMap(), state.generationSpeedMs(), false,
              newLastGenerationTime, state.cellSize(), state.allPopulationPaths());
        }
        break;
      case CLOSE_DROPDOWNS:
        state = new GAAnimationState(
            state.maze(), state.isPlaying(), state.currentGeneration(), state.currentBestPath(),
            state.currentBestFitness(), state.goalReached(), state.selectedMazeFileName(), false,
            state.availableMazeFiles(), state.mazeValidityMap(), state.generationSpeedMs(), false,
            state.lastGenerationTime(), state.cellSize(), state.allPopulationPaths());
        break;
      default:
        break;
    }
  }

  public void updateAnimation() {
    if (!state.isPlaying() || state.maze() == null || gaRunner == null) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - state.lastGenerationTime() >= state.generationSpeedMs()) {
      GARunner.GenerationResult result = gaRunner.nextGeneration();
      boolean shouldStop = result.goalReached() || result.generation() >= GUIConstants.MAX_GENERATIONS;

      state = new GAAnimationState(
          state.maze(), !shouldStop, result.generation(), result.bestPath(),
          result.bestFitness(), result.goalReached(), state.selectedMazeFileName(), false,
          state.availableMazeFiles(), state.mazeValidityMap(), state.generationSpeedMs(), false,
          currentTime, state.cellSize(), result.allPaths());
    }
  }

  public void render() {
    view.render(state);
  }

  private void loadMaze(String fileName) {
    if (fileName.equals(state.selectedMazeFileName())) {
      return;
    }

    Maze maze = MazeStore.getMaze(fileName);
    int cellSize = calculateCellSize(maze);
    gaRunner = new GARunner(maze);
    GARunner.GenerationResult initial = gaRunner.getCurrentState();

    state = new GAAnimationState(
        maze, false, 0, initial.bestPath(), initial.bestFitness(), initial.goalReached(),
        fileName, false, state.availableMazeFiles(), state.mazeValidityMap(), state.generationSpeedMs(),
        false, 0, cellSize, initial.allPaths());
  }

  private int calculateCellSize(Maze maze) {
    if (maze == null) {
      return 0;
    }
    int availableWidth = GUIConstants.MAZE_PANEL_WIDTH - 2 * GUIConstants.PADDING;
    int availableHeight = GUIConstants.MAZE_PANEL_HEIGHT - 2 * GUIConstants.PADDING;
    int cellSizeByWidth = availableWidth / maze.getWidth();
    int cellSizeByHeight = availableHeight / maze.getHeight();
    int cellSize = Math.min(cellSizeByWidth, cellSizeByHeight);
    return Math.max(1, cellSize);
  }

  public GAAnimationState getState() {
    return state;
  }
}
