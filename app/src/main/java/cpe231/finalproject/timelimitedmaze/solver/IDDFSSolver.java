package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IDDFSSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "IDDFS (Iterative Deepening DFS)";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("IDDFS start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int maxDepth = rows * cols;
    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int totalExpansions = 0;

    for (int depth = 0; depth <= maxDepth; depth++) {
      boolean[] visited = new boolean[rows * cols];
      List<Coordinate> path = new ArrayList<>();
      path.add(start);

      int expansions = depthLimitedSearch(
          grid, rows, cols, startRow, startCol, goalRow, goalCol,
          depth, visited, path, dRow, dCol);

      totalExpansions += expansions;

      if (path.get(path.size() - 1).row() == goalRow &&
          path.get(path.size() - 1).column() == goalCol) {
        log("IDDFS found path at depth " + depth + " after " + totalExpansions + " total expansions");
        return path;
      }
    }

    log("IDDFS exhausted search after " + totalExpansions + " expansions with no path");
    throw new MazeSolvingException("No path found from start to goal");
  }

  private int depthLimitedSearch(
      List<List<MazeCell>> grid, int rows, int cols,
      int r, int c, int goalRow, int goalCol,
      int depth, boolean[] visited, List<Coordinate> path,
      int[] dRow, int[] dCol) {

    if (depth == 0) {
      return 1;
    }

    int currentIndex = r * cols + c;
    visited[currentIndex] = true;
    int expansions = 1;

    for (int k = 0; k < 4; k++) {
      int nr = r + dRow[k];
      int nc = c + dCol[k];

      if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
        int neighborIndex = nr * cols + nc;

        if (visited[neighborIndex]) {
          continue;
        }

        MazeCell cell = grid.get(nr).get(nc);
        if (!cell.isWalkable()) {
          continue;
        }

        if (nr == goalRow && nc == goalCol) {
          path.add(new Coordinate(nr, nc));
          return expansions;
        }

        path.add(new Coordinate(nr, nc));
        int result = depthLimitedSearch(
            grid, rows, cols, nr, nc, goalRow, goalCol,
            depth - 1, visited, path, dRow, dCol);

        expansions += result;

        if (path.get(path.size() - 1).row() == goalRow &&
            path.get(path.size() - 1).column() == goalCol) {
          return expansions;
        }

        path.remove(path.size() - 1);
      }
    }

    visited[currentIndex] = false;
    return expansions;
  }
}

