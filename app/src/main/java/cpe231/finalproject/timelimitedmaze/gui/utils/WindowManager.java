package cpe231.finalproject.timelimitedmaze.gui.utils;

import com.raylib.Raylib;

public abstract class WindowManager {
  protected final int width;
  protected final int height;
  protected final String title;

  protected WindowManager(int width, int height, String title) {
    this.width = width;
    this.height = height;
    this.title = title;
  }

  public void show() {
    System.out.println("Initializing GUI window: " + width + "x" + height);
    Raylib.InitWindow(width, height, title);

    if (!Raylib.IsWindowReady()) {
      System.err.println("ERROR: Window failed to initialize!");
      return;
    }

    System.out.println("Window initialized successfully");
    Raylib.SetTargetFPS(60);

    initialize();

    while (!Raylib.WindowShouldClose()) {
      if (Raylib.IsKeyPressed(Raylib.KEY_ESCAPE)) {
        System.out.println("ESC pressed, closing window");
        break;
      }

      handleInput();

      Raylib.BeginDrawing();
      Raylib.ClearBackground(GUIConstants.BACKGROUND_COLOR);

      render();

      Raylib.EndDrawing();
    }

    System.out.println("Closing window");
    cleanup();
    Raylib.CloseWindow();
  }

  protected void initialize() {
  }

  protected abstract void handleInput();

  protected abstract void render();

  protected void cleanup() {
  }
}
