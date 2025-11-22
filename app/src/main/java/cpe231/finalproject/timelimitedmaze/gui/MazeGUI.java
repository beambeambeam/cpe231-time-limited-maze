package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolvingException;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeValidator;
import cpe231.finalproject.timelimitedmaze.utils.SolverRegistry;
import com.raylib.Raylib;
import com.raylib.Helpers;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class MazeGUI {
  private Maze maze;
  private final MazeVisualizer visualizer;
  private List<MazeSolver> availableSolvers;
  private Integer selectedSolverIndex;
  private SolverResult result;
  private boolean dropdownOpen;
  private String errorMessage;
  private String selectedMazeFileName;
  private boolean mazeDropdownOpen;
  private List<String> availableMazeFiles;
  private Map<String, Boolean> mazeValidityMap;

  public MazeGUI(Maze maze) {
    this.maze = maze;
    this.visualizer = new MazeVisualizer(maze, null, null);
    this.availableSolvers = SolverRegistry.getAvailableSolvers();
    this.selectedSolverIndex = null;
    this.result = null;
    this.dropdownOpen = false;
    this.errorMessage = null;
    this.selectedMazeFileName = maze.getName();
    this.mazeDropdownOpen = false;
    this.availableMazeFiles = MazeFileLister.listMazeFiles();
    this.mazeValidityMap = new HashMap<>();
    for (String fileName : availableMazeFiles) {
      mazeValidityMap.put(fileName, MazeValidator.isValidMaze(fileName));
    }
  }

  public void show() {
    int width = visualizer.getWindowWidth();
    int height = visualizer.getWindowHeight();

    System.out.println("Initializing GUI window: " + width + "x" + height);
    Raylib.InitWindow(width, height, "Maze Solver");

    if (!Raylib.IsWindowReady()) {
      System.err.println("ERROR: Window failed to initialize!");
      return;
    }

    System.out.println("Window initialized successfully");
    Raylib.SetTargetFPS(60);

    visualizer.initializeTexture();

    while (!Raylib.WindowShouldClose()) {
      if (Raylib.IsKeyPressed(Raylib.KEY_ESCAPE)) {
        System.out.println("ESC pressed, closing window");
        break;
      }

      handleMouseInput();

      Raylib.BeginDrawing();
      Raylib.ClearBackground(Helpers.newColor((byte) 20, (byte) 20, (byte) 20, (byte) 255));

      visualizer.render(availableSolvers, selectedSolverIndex, dropdownOpen, result, errorMessage,
          selectedMazeFileName, mazeDropdownOpen, availableMazeFiles, mazeValidityMap);

      Raylib.EndDrawing();
    }

    System.out.println("Closing window");
    Raylib.CloseWindow();
  }

  private void handleMouseInput() {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    if (Raylib.IsMouseButtonPressed(Raylib.MOUSE_BUTTON_LEFT)) {
      Integer algorithmClicked = visualizer.checkDropdownClick(mousePos, dropdownOpen, availableSolvers);
      Integer mazeClicked = visualizer.checkMazeDropdownClick(mousePos, mazeDropdownOpen, availableMazeFiles,
          mazeValidityMap);

      if (algorithmClicked != null) {
        if (algorithmClicked == -1) {
          dropdownOpen = !dropdownOpen;
          mazeDropdownOpen = false;
        } else {
          Integer newSelection = algorithmClicked;

          if (selectedSolverIndex == null || !selectedSolverIndex.equals(newSelection)) {
            selectedSolverIndex = newSelection;
            solveMaze();
          }
          dropdownOpen = false;
          mazeDropdownOpen = false;
        }
      } else if (mazeClicked != null) {
        if (mazeClicked == -1) {
          mazeDropdownOpen = !mazeDropdownOpen;
          dropdownOpen = false;
        } else {
          String selectedFileName = availableMazeFiles.get(mazeClicked);
          if (mazeValidityMap.getOrDefault(selectedFileName, false)) {
            loadMaze(selectedFileName);
          }
          mazeDropdownOpen = false;
          dropdownOpen = false;
        }
      } else {
        if (dropdownOpen || mazeDropdownOpen) {
          dropdownOpen = false;
          mazeDropdownOpen = false;
        }
      }
    }
  }

  private void loadMaze(String fileName) {
    if (fileName.equals(selectedMazeFileName)) {
      return;
    }

    selectedMazeFileName = fileName;
    maze = MazeStore.getMaze(fileName);
    visualizer.updateMaze(maze);
    result = null;
    errorMessage = null;

    if (selectedSolverIndex != null) {
      solveMaze();
    }
  }

  private void solveMaze() {
    if (selectedSolverIndex == null) {
      return;
    }

    errorMessage = null;
    try {
      MazeSolver solver = availableSolvers.get(selectedSolverIndex);
      result = solver.solve(maze);
    } catch (MazeSolvingException exception) {
      errorMessage = "Failed to solve maze: " + exception.getMessage();
      result = null;
    }
  }
}
