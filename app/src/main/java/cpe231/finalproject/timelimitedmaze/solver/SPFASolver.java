package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public final class SPFASolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "SPFA (Shortest Path Faster Algorithm)";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("SPFA start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    double[] dist = new double[size];
    int[] parent = new int[size];
    boolean[] inQueue = new boolean[size];

    for (int i = 0; i < size; i++) {
      dist[i] = Double.POSITIVE_INFINITY;
      parent[i] = -1;
    }

    int startIndex = startRow * cols + startCol;
    dist[startIndex] = 0.0;

    Queue<Node> queue = new ArrayDeque<>();
    queue.add(new Node(startRow, startCol));
    inQueue[startIndex] = true;

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int expansions = 0;

    while (!queue.isEmpty()) {
      Node current = queue.poll();
      int r = current.r;
      int c = current.c;
      int currentIndex = r * cols + c;
      inQueue[currentIndex] = false;

      if (r == goalRow && c == goalCol) {
        return reconstructPath(parent, goalRow, goalCol, cols);
      }

      for (int k = 0; k < 4; k++) {
        int nr = r + dRow[k];
        int nc = c + dCol[k];

        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
          int neighborIndex = nr * cols + nc;

          MazeCell cell = grid.get(nr).get(nc);
          if (!cell.isWalkable()) {
            continue;
          }

          double stepCost = cell.stepCost();
          double newDist = dist[currentIndex] + stepCost;

          if (newDist < dist[neighborIndex]) {
            dist[neighborIndex] = newDist;
            parent[neighborIndex] = currentIndex;

            if (!inQueue[neighborIndex]) {
              queue.add(new Node(nr, nc));
              inQueue[neighborIndex] = true;
            }
          }
        }
      }
      expansions++;
    }

    log("SPFA exhausted search after expanding " + expansions + " nodes with no path");
    throw new MazeSolvingException("No path found from start to goal");
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
    log("SPFA reconstructed path of length " + path.size());
    return path;
  }

  private static class Node {
    int r, c;

    Node(int r, int c) {
      this.r = r;
      this.c = c;
    }
  }
}

