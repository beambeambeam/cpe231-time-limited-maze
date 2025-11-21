package cpe231.finalproject.timelimitedmaze;

import cpe231.finalproject.timelimitedmaze.gui.MazeGUI;
import cpe231.finalproject.timelimitedmaze.solver.LeftWallFollowerSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolvingException;
import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;

public final class App {
  private App() {
  }

  public static void main(String[] args) {
    Maze maze = MazeStore.getMaze("m100_100.txt");

    MazeSolver leftWallFollowerSolver = new LeftWallFollowerSolver();
    try {
      SolverResult result = leftWallFollowerSolver.solve(maze);
      MazeGUI gui = new MazeGUI(maze, result);
      gui.show();
    } catch (MazeSolvingException exception) {
      System.err.println("Failed to solve maze: " + exception.getMessage());
    }
  }
}
