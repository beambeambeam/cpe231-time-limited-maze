package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public final class DFSSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "DFS (Depth-First Search)";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("DFS start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    int[] parent = new int[size];
    boolean[] visited = new boolean[size];

    for (int i = 0; i < size; i++) {
      parent[i] = -1;
    }

    int startIndex = startRow * cols + startCol;
    visited[startIndex] = true;

    Stack<Node> stack = new Stack<>();
    stack.push(new Node(startRow, startCol));

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int expansions = 0;

    while (!stack.isEmpty()) {
      Node current = stack.pop();
      int r = current.r;
      int c = current.c;
      int currentIndex = r * cols + c;

      if (r == goalRow && c == goalCol) {
        return reconstructPath(parent, goalRow, goalCol, cols);
      }

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

          visited[neighborIndex] = true;
          parent[neighborIndex] = currentIndex;
          stack.push(new Node(nr, nc));
        }
      }
      expansions++;
    }

    log("DFS exhausted search after expanding " + expansions + " nodes with no path");
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
    log("DFS reconstructed path of length " + path.size());
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

