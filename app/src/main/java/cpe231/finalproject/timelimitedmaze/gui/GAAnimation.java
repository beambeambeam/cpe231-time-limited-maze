package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.solver.generic.FitnessCalculator;
import cpe231.finalproject.timelimitedmaze.solver.generic.Individual;
import cpe231.finalproject.timelimitedmaze.solver.generic.IntersectionCrossover;
import cpe231.finalproject.timelimitedmaze.solver.generic.MutationOperator;
import cpe231.finalproject.timelimitedmaze.solver.generic.PathChromosome;
import cpe231.finalproject.timelimitedmaze.solver.generic.Population;
import cpe231.finalproject.timelimitedmaze.solver.generic.PopulationInitializer;
import cpe231.finalproject.timelimitedmaze.solver.generic.TournamentSelection;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import cpe231.finalproject.timelimitedmaze.utils.MazeCellType;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import cpe231.finalproject.timelimitedmaze.utils.MazeValidator;
import com.raylib.Raylib;
import com.raylib.Colors;
import com.raylib.Helpers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class GAAnimation {
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 800;
  private static final int STATS_PANEL_WIDTH = 300;
  private static final int MAZE_PANEL_WIDTH = WINDOW_WIDTH - STATS_PANEL_WIDTH;
  private static final int MAZE_PANEL_HEIGHT = WINDOW_HEIGHT;
  private static final int PADDING = 20;
  private static final int DROPDOWN_WIDTH = 250;
  private static final int DROPDOWN_HEIGHT = 30;
  private static final int DROPDOWN_OPTION_HEIGHT = 30;
  private static final int BUTTON_WIDTH = 120;
  private static final int BUTTON_HEIGHT = 40;
  private static final int MAX_GENERATIONS = 200;
  private static final int POPULATION_SIZE = 500;
  private static final double ELITE_PERCENTAGE = 0.05;

  private Maze maze;
  private GARunner gaRunner;
  private boolean isPlaying;
  private int currentGeneration;
  private List<Coordinate> currentBestPath;
  private double currentBestFitness;
  private boolean goalReached;
  private String selectedMazeFileName;
  private boolean mazeDropdownOpen;
  private List<String> availableMazeFiles;
  private Map<String, Boolean> mazeValidityMap;
  private int generationSpeedMs;
  private boolean speedDropdownOpen;
  private long lastGenerationTime;
  private int cellSize;

  public GAAnimation() {
    this.availableMazeFiles = MazeFileLister.listMazeFiles();
    this.mazeValidityMap = new java.util.HashMap<>();
    for (String fileName : availableMazeFiles) {
      mazeValidityMap.put(fileName, MazeValidator.isValidMaze(fileName));
    }
    this.selectedMazeFileName = null;
    this.maze = null;
    this.gaRunner = null;
    this.isPlaying = false;
    this.currentGeneration = 0;
    this.currentBestPath = new ArrayList<>();
    this.currentBestFitness = Double.NEGATIVE_INFINITY;
    this.goalReached = false;
    this.mazeDropdownOpen = false;
    this.generationSpeedMs = 500;
    this.speedDropdownOpen = false;
    this.lastGenerationTime = 0;
  }

  public static void main(String[] args) {
    GAAnimation animation = new GAAnimation();
    animation.show();
  }

  public void show() {
    System.out.println("Initializing GA Animation window: " + WINDOW_WIDTH + "x" + WINDOW_HEIGHT);
    Raylib.InitWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "GA Animation");

    if (!Raylib.IsWindowReady()) {
      System.err.println("ERROR: Window failed to initialize!");
      return;
    }

    System.out.println("Window initialized successfully");
    Raylib.SetTargetFPS(60);

    while (!Raylib.WindowShouldClose()) {
      if (Raylib.IsKeyPressed(Raylib.KEY_ESCAPE)) {
        System.out.println("ESC pressed, closing window");
        break;
      }

      handleMouseInput();
      updateAnimation();

      Raylib.BeginDrawing();
      Raylib.ClearBackground(Helpers.newColor((byte) 20, (byte) 20, (byte) 20, (byte) 255));

      renderMaze();
      renderPath();
      renderStartAndGoal();
      renderStatistics();
      renderControls();
      renderDropdowns();

      Raylib.EndDrawing();
    }

    System.out.println("Closing window");
    Raylib.CloseWindow();
  }

  private void handleMouseInput() {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    if (Raylib.IsMouseButtonPressed(Raylib.MOUSE_BUTTON_LEFT)) {
      Integer mazeClicked = checkMazeDropdownClick(mousePos, mazeDropdownOpen, availableMazeFiles, mazeValidityMap);
      Integer speedClicked = checkSpeedDropdownClick(mousePos, speedDropdownOpen);
      boolean playStopClicked = checkPlayStopButtonClick(mousePos);

      if (mazeClicked != null) {
        if (mazeClicked == -1) {
          mazeDropdownOpen = !mazeDropdownOpen;
          speedDropdownOpen = false;
        } else {
          String selectedFileName = availableMazeFiles.get(mazeClicked);
          if (mazeValidityMap.getOrDefault(selectedFileName, false)) {
            loadMaze(selectedFileName);
          }
          mazeDropdownOpen = false;
          speedDropdownOpen = false;
        }
      } else if (speedClicked != null) {
        if (speedClicked == -1) {
          speedDropdownOpen = !speedDropdownOpen;
          mazeDropdownOpen = false;
        } else {
          int[] speeds = { 100, 500, 1000 };
          if (speedClicked >= 0 && speedClicked < speeds.length) {
            generationSpeedMs = speeds[speedClicked];
          }
          speedDropdownOpen = false;
          mazeDropdownOpen = false;
        }
      } else if (playStopClicked) {
        if (maze != null) {
          isPlaying = !isPlaying;
          if (isPlaying) {
            lastGenerationTime = System.currentTimeMillis();
          }
        }
        mazeDropdownOpen = false;
        speedDropdownOpen = false;
      } else {
        if (mazeDropdownOpen || speedDropdownOpen) {
          mazeDropdownOpen = false;
          speedDropdownOpen = false;
        }
      }
    }
  }

  private void updateAnimation() {
    if (!isPlaying || maze == null || gaRunner == null) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastGenerationTime >= generationSpeedMs) {
      GenerationResult result = gaRunner.nextGeneration();
      currentGeneration = result.generation();
      currentBestPath = result.bestPath();
      currentBestFitness = result.bestFitness();
      goalReached = result.goalReached();

      if (goalReached || currentGeneration >= MAX_GENERATIONS) {
        isPlaying = false;
      }

      lastGenerationTime = currentTime;
    }
  }

  private void loadMaze(String fileName) {
    if (fileName.equals(selectedMazeFileName)) {
      return;
    }

    selectedMazeFileName = fileName;
    maze = MazeStore.getMaze(fileName);
    calculateCellSize();
    resetToGenerationOne();
  }

  private void resetToGenerationOne() {
    if (maze == null) {
      return;
    }

    gaRunner = new GARunner(maze);
    currentGeneration = 0;
    GenerationResult initial = gaRunner.getCurrentState();
    currentBestPath = initial.bestPath();
    currentBestFitness = initial.bestFitness();
    goalReached = initial.goalReached();
    isPlaying = false;
    lastGenerationTime = 0;
  }

  private void calculateCellSize() {
    if (maze == null) {
      return;
    }
    int availableWidth = MAZE_PANEL_WIDTH - 2 * PADDING;
    int availableHeight = MAZE_PANEL_HEIGHT - 2 * PADDING;
    int cellSizeByWidth = availableWidth / maze.getWidth();
    int cellSizeByHeight = availableHeight / maze.getHeight();
    this.cellSize = Math.min(cellSizeByWidth, cellSizeByHeight);
    this.cellSize = Math.max(1, this.cellSize);
  }

  private void renderMaze() {
    if (maze == null) {
      return;
    }

    int offsetX = PADDING;
    int offsetY = PADDING;

    Set<Coordinate> pathSet = new HashSet<>(currentBestPath);

    for (int row = 0; row < maze.getHeight(); row++) {
      for (int col = 0; col < maze.getWidth(); col++) {
        Coordinate coord = new Coordinate(row, col);
        MazeCell cell = maze.getCell(coord);
        int x = offsetX + col * cellSize;
        int y = offsetY + row * cellSize;

        Raylib.Color cellColor = getCellColor(cell, coord, pathSet);
        Raylib.DrawRectangle(x, y, cellSize, cellSize, cellColor);

        if (cellSize > 8 && cell.type() == MazeCellType.WEIGHTED && cell.weight().isPresent()) {
          String weightText = String.valueOf(cell.weight().getAsInt());
          int fontSize = Math.max(8, cellSize / 3);
          Raylib.DrawText(weightText, x + 2, y + 2, fontSize, Colors.BLACK);
        }
      }
    }
  }

  private Raylib.Color getCellColor(MazeCell cell, Coordinate coord, Set<Coordinate> pathSet) {
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
    if (currentBestPath == null || currentBestPath.size() < 2) {
      return;
    }

    int offsetX = PADDING;
    int offsetY = PADDING;
    int centerOffset = cellSize / 2;

    for (int i = 0; i < currentBestPath.size() - 1; i++) {
      Coordinate from = currentBestPath.get(i);
      Coordinate to = currentBestPath.get(i + 1);

      int fromX = offsetX + from.column() * cellSize + centerOffset;
      int fromY = offsetY + from.row() * cellSize + centerOffset;
      int toX = offsetX + to.column() * cellSize + centerOffset;
      int toY = offsetY + to.row() * cellSize + centerOffset;

      Raylib.DrawLine(fromX, fromY, toX, toY, Helpers.newColor((byte) 0, (byte) 100, (byte) 255, (byte) 200));
    }
  }

  private void renderStartAndGoal() {
    if (maze == null) {
      return;
    }

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

  private void renderStatistics() {
    int panelX = MAZE_PANEL_WIDTH;
    int panelY = PADDING;
    int lineHeight = 30;
    int currentY = panelY;

    Raylib.DrawRectangle(panelX, 0, STATS_PANEL_WIDTH, WINDOW_HEIGHT,
        Helpers.newColor((byte) 35, (byte) 35, (byte) 40, (byte) 255));

    String title = "GA Statistics";
    Raylib.DrawText(title, panelX + 10, currentY, 24, Colors.WHITE);
    currentY += lineHeight + 10;

    if (maze != null) {
      String mazeName = "Maze: " + maze.getName();
      Raylib.DrawText(mazeName, panelX + 10, currentY, 18, Colors.LIGHTGRAY);
      currentY += lineHeight;

      String dimensions = String.format("Size: %d x %d", maze.getWidth(), maze.getHeight());
      Raylib.DrawText(dimensions, panelX + 10, currentY, 18, Colors.LIGHTGRAY);
      currentY += lineHeight + 10;

      String generationText = "Generation: " + currentGeneration;
      Raylib.DrawText(generationText, panelX + 10, currentY, 20, Colors.BLUE);
      currentY += lineHeight;

      String fitnessText = String.format("Best Fitness: %.2f", currentBestFitness);
      Raylib.DrawText(fitnessText, panelX + 10, currentY, 18, Colors.LIGHTGRAY);
      currentY += lineHeight;

      String pathLengthText = "Path Length: " + currentBestPath.size();
      Raylib.DrawText(pathLengthText, panelX + 10, currentY, 18, Colors.LIGHTGRAY);
      currentY += lineHeight;

      String goalStatus = goalReached ? "Goal: REACHED" : "Goal: Not reached";
      Raylib.Color goalColor = goalReached ? Colors.GREEN : Colors.YELLOW;
      Raylib.DrawText(goalStatus, panelX + 10, currentY, 18, goalColor);
      currentY += lineHeight + 20;
    } else {
      String noMazeText = "Select a maze to begin";
      Raylib.DrawText(noMazeText, panelX + 10, currentY, 18, Colors.LIGHTGRAY);
      currentY += lineHeight + 20;
    }

    String legend = "Legend:";
    Raylib.DrawText(legend, panelX + 10, currentY, 18, Colors.WHITE);
    currentY += lineHeight;

    int legendItemSize = 15;
    int legendX = panelX + 20;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.GREEN);
    Raylib.DrawText("Start", legendX + legendItemSize + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.RED);
    Raylib.DrawText("Goal", legendX + legendItemSize + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.BLUE);
    Raylib.DrawText("Path", legendX + legendItemSize + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.DARKGRAY);
    Raylib.DrawText("Wall", legendX + legendItemSize + 5, currentY, 16, Colors.WHITE);
    currentY += lineHeight;

    Raylib.DrawRectangle(legendX, currentY, legendItemSize, legendItemSize, Colors.LIGHTGRAY);
    Raylib.DrawText("Walkable", legendX + legendItemSize + 5, currentY, 16, Colors.WHITE);
  }

  private void renderControls() {
    int panelX = MAZE_PANEL_WIDTH;
    int buttonX = panelX + 10;
    int buttonY = WINDOW_HEIGHT - BUTTON_HEIGHT - 20;

    String buttonText = isPlaying ? "Stop" : "Play";
    Raylib.Color buttonColor = isPlaying
        ? Helpers.newColor((byte) 200, (byte) 50, (byte) 50, (byte) 255)
        : Helpers.newColor((byte) 50, (byte) 200, (byte) 50, (byte) 255);

    Raylib.DrawRectangle(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, buttonColor);
    Raylib.DrawRectangleLines(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
        Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));

    int textWidth = Raylib.MeasureText(buttonText, 18);
    int textX = buttonX + (BUTTON_WIDTH - textWidth) / 2;
    int textY = buttonY + (BUTTON_HEIGHT - 18) / 2;
    Raylib.DrawText(buttonText, textX, textY, 18, Colors.WHITE);
  }

  private void renderDropdowns() {
    int panelX = MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int mazeDropdownY = PADDING + 30 + 10;

    int maxMazeOptionsHeight = 0;
    int validCount = 0;
    for (String fileName : availableMazeFiles) {
      if (mazeValidityMap.getOrDefault(fileName, false)) {
        validCount++;
      }
    }
    maxMazeOptionsHeight = validCount * DROPDOWN_OPTION_HEIGHT;

    int speedDropdownY = mazeDropdownY + DROPDOWN_HEIGHT + maxMazeOptionsHeight + 10;

    renderMazeDropdown(dropdownX, mazeDropdownY, selectedMazeFileName, mazeDropdownOpen,
        availableMazeFiles, mazeValidityMap);
    renderSpeedDropdown(dropdownX, speedDropdownY, generationSpeedMs, speedDropdownOpen);
  }

  private void renderMazeDropdown(int x, int y, String selectedMazeFileName, boolean isOpen,
      List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    String displayText = selectedMazeFileName == null
        ? "Select Maze..."
        : selectedMazeFileName;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    Raylib.DrawRectangle((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        Helpers.newColor((byte) 50, (byte) 50, (byte) 55, (byte) 255));
    Raylib.DrawRectangleLines((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));

    Raylib.DrawText(displayText, x + 5, y + 6, 16, Colors.WHITE);

    int arrowX = x + DROPDOWN_WIDTH - 20;
    int arrowY = y + DROPDOWN_HEIGHT / 2;
    if (isOpen) {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY - 5),
          Helpers.newVector2(arrowX + 10, arrowY - 5),
          Helpers.newVector2(arrowX + 5, arrowY + 5),
          Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));
    } else {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY + 5),
          Helpers.newVector2(arrowX + 10, arrowY + 5),
          Helpers.newVector2(arrowX + 5, arrowY - 5),
          Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));
    }

    if (isOpen) {
      int validCount = 0;
      for (String fileName : availableMazeFiles) {
        if (mazeValidityMap.getOrDefault(fileName, false)) {
          validCount++;
        }
      }
      int totalOptionsHeight = validCount * DROPDOWN_OPTION_HEIGHT;
      int optionsStartY = y + DROPDOWN_HEIGHT;

      Raylib.DrawRectangle(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight,
          Helpers.newColor((byte) 50, (byte) 50, (byte) 55, (byte) 255));
      Raylib.DrawRectangleLines(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight,
          Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));

      int optionIndex = 0;
      for (int i = 0; i < availableMazeFiles.size(); i++) {
        String fileName = availableMazeFiles.get(i);
        boolean isValid = mazeValidityMap.getOrDefault(fileName, false);
        if (!isValid) {
          continue;
        }
        int optionY = optionsStartY + optionIndex * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        boolean isHovered = Raylib.CheckCollisionPointRec(mousePos, optionRect);

        if (isHovered) {
          Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
              (int) optionRect.width(), (int) optionRect.height(),
              Helpers.newColor((byte) 60, (byte) 60, (byte) 65, (byte) 255));
        }

        Raylib.DrawText(fileName, x + 5, optionY + 6, 16, Colors.WHITE);
        optionIndex++;
      }
    }
  }

  private void renderSpeedDropdown(int x, int y, int currentSpeed, boolean isOpen) {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    String displayText = currentSpeed + " ms/gen";
    String[] speedOptions = { "100", "500", "1000" };

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(x, y, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    Raylib.DrawRectangle((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        Helpers.newColor((byte) 50, (byte) 50, (byte) 55, (byte) 255));
    Raylib.DrawRectangleLines((int) dropdownRect.x(), (int) dropdownRect.y(),
        (int) dropdownRect.width(), (int) dropdownRect.height(),
        Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));

    Raylib.DrawText(displayText, x + 5, y + 6, 16, Colors.WHITE);

    int arrowX = x + DROPDOWN_WIDTH - 20;
    int arrowY = y + DROPDOWN_HEIGHT / 2;
    if (isOpen) {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY - 5),
          Helpers.newVector2(arrowX + 10, arrowY - 5),
          Helpers.newVector2(arrowX + 5, arrowY + 5),
          Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));
    } else {
      Raylib.DrawTriangle(Helpers.newVector2(arrowX, arrowY + 5),
          Helpers.newVector2(arrowX + 10, arrowY + 5),
          Helpers.newVector2(arrowX + 5, arrowY - 5),
          Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));
    }

    if (isOpen) {
      int totalOptionsHeight = speedOptions.length * DROPDOWN_OPTION_HEIGHT;
      int optionsStartY = y + DROPDOWN_HEIGHT;

      Raylib.DrawRectangle(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight,
          Helpers.newColor((byte) 50, (byte) 50, (byte) 55, (byte) 255));
      Raylib.DrawRectangleLines(x, optionsStartY, DROPDOWN_WIDTH, totalOptionsHeight,
          Helpers.newColor((byte) 100, (byte) 100, (byte) 100, (byte) 255));

      for (int i = 0; i < speedOptions.length; i++) {
        int optionY = optionsStartY + i * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(x, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        boolean isHovered = Raylib.CheckCollisionPointRec(mousePos, optionRect);

        if (isHovered) {
          Raylib.DrawRectangle((int) optionRect.x(), (int) optionRect.y(),
              (int) optionRect.width(), (int) optionRect.height(),
              Helpers.newColor((byte) 60, (byte) 60, (byte) 65, (byte) 255));
        }

        String optionText = speedOptions[i] + " ms/gen";
        Raylib.DrawText(optionText, x + 5, optionY + 6, 16, Colors.WHITE);
      }
    }
  }

  private Integer checkMazeDropdownClick(Raylib.Vector2 mousePos, boolean isOpen,
      List<String> availableMazeFiles, Map<String, Boolean> mazeValidityMap) {
    int panelX = MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int dropdownY = PADDING + 30 + 10;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(dropdownX, dropdownY, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    if (Raylib.CheckCollisionPointRec(mousePos, dropdownRect)) {
      return -1;
    }

    if (isOpen) {
      int validIndex = 0;
      for (int i = 0; i < availableMazeFiles.size(); i++) {
        String fileName = availableMazeFiles.get(i);
        if (!mazeValidityMap.getOrDefault(fileName, false)) {
          continue;
        }
        int optionY = dropdownY + DROPDOWN_HEIGHT + validIndex * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(dropdownX, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        if (Raylib.CheckCollisionPointRec(mousePos, optionRect)) {
          return i;
        }
        validIndex++;
      }
    }

    return null;
  }

  private Integer checkSpeedDropdownClick(Raylib.Vector2 mousePos, boolean isOpen) {
    int panelX = MAZE_PANEL_WIDTH;
    int dropdownX = panelX + 10;
    int mazeDropdownY = PADDING + 30 + 10;

    int maxMazeOptionsHeight = 0;
    int validCount = 0;
    for (String fileName : availableMazeFiles) {
      if (mazeValidityMap.getOrDefault(fileName, false)) {
        validCount++;
      }
    }
    maxMazeOptionsHeight = validCount * DROPDOWN_OPTION_HEIGHT;

    int dropdownY = mazeDropdownY + DROPDOWN_HEIGHT + maxMazeOptionsHeight + 10;

    Raylib.Rectangle dropdownRect = Helpers.newRectangle(dropdownX, dropdownY, DROPDOWN_WIDTH, DROPDOWN_HEIGHT);

    if (Raylib.CheckCollisionPointRec(mousePos, dropdownRect)) {
      return -1;
    }

    if (isOpen) {
      for (int i = 0; i < 3; i++) {
        int optionY = dropdownY + DROPDOWN_HEIGHT + i * DROPDOWN_OPTION_HEIGHT;
        Raylib.Rectangle optionRect = Helpers.newRectangle(dropdownX, optionY, DROPDOWN_WIDTH, DROPDOWN_OPTION_HEIGHT);

        if (Raylib.CheckCollisionPointRec(mousePos, optionRect)) {
          return i;
        }
      }
    }

    return null;
  }

  private boolean checkPlayStopButtonClick(Raylib.Vector2 mousePos) {
    int panelX = MAZE_PANEL_WIDTH;
    int buttonX = panelX + 10;
    int buttonY = WINDOW_HEIGHT - BUTTON_HEIGHT - 20;

    Raylib.Rectangle buttonRect = Helpers.newRectangle(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
    return Raylib.CheckCollisionPointRec(mousePos, buttonRect);
  }

  private static final class GARunner {
    private final Maze maze;
    private final FitnessCalculator fitnessCalculator;
    private final PopulationInitializer initializer;
    private final TournamentSelection selection;
    private final IntersectionCrossover crossover;
    private final MutationOperator mutation;
    private Population population;
    private int generation;
    private double bestFitness;
    private PathChromosome bestChromosome;

    GARunner(Maze maze) {
      this.maze = maze;
      this.fitnessCalculator = new FitnessCalculator(maze);
      this.initializer = new PopulationInitializer(new Random());
      this.selection = new TournamentSelection(new Random());
      this.crossover = new IntersectionCrossover(new Random());
      this.mutation = new MutationOperator(new Random());
      this.generation = 0;
      this.bestFitness = Double.NEGATIVE_INFINITY;
      this.bestChromosome = null;
      initializePopulation();
    }

    private void initializePopulation() {
      List<PathChromosome> chromosomes = initializer.initialize(maze, POPULATION_SIZE);
      List<Individual> individuals = new ArrayList<>();

      for (PathChromosome chromosome : chromosomes) {
        PathChromosome extended = chromosome.extendTowardGoal();
        double fitness = fitnessCalculator.calculateFitness(extended);
        individuals.add(new Individual(extended, fitness));
      }

      this.population = Population.from(individuals);
      this.population.sortByFitness();

      Individual best = population.getBest();
      this.bestFitness = best.fitness();
      this.bestChromosome = best.chromosome();
    }

    GenerationResult getCurrentState() {
      List<Coordinate> bestPath = bestChromosome != null ? bestChromosome.getPath() : new ArrayList<>();
      boolean goalReached = bestChromosome != null && reachesGoal(bestPath, maze);
      return new GenerationResult(generation, bestPath, bestFitness, goalReached);
    }

    GenerationResult nextGeneration() {
      if (generation >= MAX_GENERATIONS) {
        return getCurrentState();
      }

      List<Individual> newIndividuals = new ArrayList<>();

      List<Individual> elite = population.getElite(ELITE_PERCENTAGE);
      newIndividuals.addAll(elite);

      while (newIndividuals.size() < POPULATION_SIZE) {
        PathChromosome parent1 = selection.select(population.getIndividuals());
        PathChromosome parent2 = selection.select(population.getIndividuals());

        List<PathChromosome> offspring = crossover.crossover(parent1, parent2);

        for (PathChromosome child : offspring) {
          PathChromosome repaired = child.repair();
          PathChromosome mutated = mutation.mutate(repaired);
          PathChromosome finalChromosome = mutated.repair();
          double fitness = fitnessCalculator.calculateFitness(finalChromosome);
          newIndividuals.add(new Individual(finalChromosome, fitness));

          if (newIndividuals.size() >= POPULATION_SIZE) {
            break;
          }
        }
      }

      population = Population.from(newIndividuals);
      population.sortByFitness();

      Individual currentBest = population.getBest();
      if (currentBest.fitness() > bestFitness) {
        bestFitness = currentBest.fitness();
        bestChromosome = currentBest.chromosome();
      }

      generation++;

      List<Coordinate> bestPath = bestChromosome != null ? bestChromosome.getPath() : new ArrayList<>();
      boolean goalReached = bestChromosome != null && reachesGoal(bestPath, maze);

      return new GenerationResult(generation, bestPath, bestFitness, goalReached);
    }

    private boolean reachesGoal(List<Coordinate> path, Maze maze) {
      if (path.isEmpty()) {
        return false;
      }
      return path.getLast().equals(maze.getGoal());
    }
  }

  private record GenerationResult(int generation, List<Coordinate> bestPath, double bestFitness, boolean goalReached) {
  }
}
