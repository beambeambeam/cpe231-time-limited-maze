package cpe231.finalproject.timelimitedmaze.gui;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import com.raylib.Raylib;
import com.raylib.Colors;

public final class MazeGUI {
  private final Maze maze;
  private final MazeVisualizer visualizer;

  public MazeGUI(Maze maze, SolverResult result, String algorithmName) {
    this.maze = maze;
    this.visualizer = new MazeVisualizer(maze, result, algorithmName);
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

      Raylib.BeginDrawing();
      Raylib.ClearBackground(Colors.RAYWHITE);

      visualizer.render();

      Raylib.EndDrawing();
    }

    System.out.println("Closing window");
    Raylib.CloseWindow();
  }
}
