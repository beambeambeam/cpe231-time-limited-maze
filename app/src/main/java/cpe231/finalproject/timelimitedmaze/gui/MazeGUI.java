package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.solver.WallFollowerSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolvingException;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import com.raylib.Raylib;
import com.raylib.Colors;

public final class MazeGUI {
  private final Maze maze;
  private final MazeVisualizer visualizer;
  private WallFollowerSolver.WallSide selectedAlgorithm;
  private SolverResult result;
  private boolean dropdownOpen;
  private String errorMessage;

  public MazeGUI(Maze maze) {
    this.maze = maze;
    this.visualizer = new MazeVisualizer(maze, null, null);
    this.selectedAlgorithm = null;
    this.result = null;
    this.dropdownOpen = false;
    this.errorMessage = null;
  }

  public void show() {
    int width = visualizer.getWindowWidth();
    int height = visualizer.getWindowHeight();
    String title = "Maze Solver - " + maze.getName();

    System.out.println("Initializing GUI window: " + width + "x" + height);
    Raylib.InitWindow(width, height, title);

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
      Raylib.ClearBackground(Colors.RAYWHITE);

      visualizer.render(selectedAlgorithm, dropdownOpen, result, errorMessage);

      Raylib.EndDrawing();
    }

    System.out.println("Closing window");
    Raylib.CloseWindow();
  }

  private void handleMouseInput() {
    Raylib.Vector2 mousePos = Raylib.GetMousePosition();

    if (Raylib.IsMouseButtonPressed(Raylib.MOUSE_BUTTON_LEFT)) {
      Integer clickedOption = visualizer.checkDropdownClick(mousePos, dropdownOpen);

      if (clickedOption != null) {
        if (clickedOption == -1) {
          dropdownOpen = !dropdownOpen;
        } else {
          WallFollowerSolver.WallSide newSelection = clickedOption == 0
              ? WallFollowerSolver.WallSide.LEFT
              : WallFollowerSolver.WallSide.RIGHT;

          if (selectedAlgorithm != newSelection) {
            selectedAlgorithm = newSelection;
            solveMaze();
          }
          dropdownOpen = false;
        }
      } else if (dropdownOpen) {
        dropdownOpen = false;
      }
    }
  }

  private void solveMaze() {
    if (selectedAlgorithm == null) {
      return;
    }

    errorMessage = null;
    try {
      MazeSolver solver = new WallFollowerSolver(selectedAlgorithm);
      result = solver.solve(maze);
    } catch (MazeSolvingException exception) {
      errorMessage = "Failed to solve maze: " + exception.getMessage();
      result = null;
    }
  }
}
