package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BellmanFordSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "Bellman-Ford";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("Bellman-Ford start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    double[] dist = new double[size];
    int[] parent = new int[size];

    for (int i = 0; i < size; i++) {
      dist[i] = Double.POSITIVE_INFINITY;
      parent[i] = -1;
    }

    int startIndex = startRow * cols + startCol;
    dist[startIndex] = 0.0;

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    for (int iteration = 0; iteration < size - 1; iteration++) {
      boolean relaxed = false;

      for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
          int currentIndex = r * cols + c;

          if (dist[currentIndex] == Double.POSITIVE_INFINITY) {
            continue;
          }

          MazeCell currentCell = grid.get(r).get(c);
          if (!currentCell.isWalkable()) {
            continue;
          }

          for (int k = 0; k < 4; k++) {
            int nr = r + dRow[k];
            int nc = c + dCol[k];

            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
              int neighborIndex = nr * cols + nc;

              MazeCell neighborCell = grid.get(nr).get(nc);
              if (!neighborCell.isWalkable()) {
                continue;
              }

              double stepCost = neighborCell.stepCost();
              double newDist = dist[currentIndex] + stepCost;

              if (newDist < dist[neighborIndex]) {
                dist[neighborIndex] = newDist;
                parent[neighborIndex] = currentIndex;
                relaxed = true;
              }
            }
          }
        }
      }

      if (!relaxed) {
        break;
      }
    }

    int goalIndex = goalRow * cols + goalCol;
    if (dist[goalIndex] == Double.POSITIVE_INFINITY) {
      log("Bellman-Ford found no path after " + (size - 1) + " iterations");
      throw new MazeSolvingException("No path found from start to goal");
    }

    return reconstructPath(parent, goalRow, goalCol, cols);
  }

  private List<Coordinate> reconstructPath(int[] parent, int gr, int gc, int cols) {
    List<Coordinate> path = new ArrayList<>();
    int currIndex = gr * cols + gc;

    while (currIndex != -1) {
      int r = currIndex / cols;
      int c = currIndex % cols;
      path.add(new Coordinate(r, c));
      currIndex = parent[currIndex];
    }
    Collections.reverse(path);
    log("Bellman-Ford reconstructed path of length " + path.size());
    return path;
  }
}

