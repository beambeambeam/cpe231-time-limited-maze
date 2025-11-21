package cpe231.finalproject.timelimitedmaze;

import cpe231.finalproject.timelimitedmaze.gui.MazeGUI;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;

public final class App {
  private App() {
  }

  public static void main(String[] args) {
    Maze maze = MazeStore.getMaze("m100_100.txt");
    MazeGUI gui = new MazeGUI(maze);
      gui.show();
  }
}
