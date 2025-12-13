package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class ThetaStarSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "Theta* (Any-Angle A*)";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("Theta* start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    double[] gScore = new double[size];
    double[] fScore = new double[size];
    int[] parent = new int[size];
    boolean[] closed = new boolean[size];

    Arrays.fill(gScore, Double.POSITIVE_INFINITY);
    Arrays.fill(fScore, Double.POSITIVE_INFINITY);
    Arrays.fill(parent, -1);

    int startIndex = startRow * cols + startCol;
    gScore[startIndex] = 0.0;
    fScore[startIndex] = calculateHValue(startRow, startCol, goalRow, goalCol);

    PriorityQueue<Node> openList = new PriorityQueue<>();
    openList.add(new Node(fScore[startIndex], startRow, startCol));

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int expansions = 0;

    while (!openList.isEmpty()) {
      Node current = openList.poll();
      int r = current.r;
      int c = current.c;
      int currentIndex = r * cols + c;

      if (closed[currentIndex]) {
        continue;
      }
      closed[currentIndex] = true;

      if (r == goalRow && c == goalCol) {
        return reconstructPath(parent, goalRow, goalCol, cols);
      }

      int parentIndex = parent[currentIndex];
      if (parentIndex != -1) {
        int parentRow = parentIndex / cols;
        int parentCol = parentIndex % cols;

        if (hasLineOfSight(grid, rows, cols, parentRow, parentCol, r, c)) {
          double directCost = calculateDirectCost(grid, rows, cols, parentRow, parentCol, r, c);
          double newG = gScore[parentIndex] + directCost;

          if (newG < gScore[currentIndex]) {
            gScore[currentIndex] = newG;
            parent[currentIndex] = parentIndex;
            fScore[currentIndex] = newG + calculateHValue(r, c, goalRow, goalCol);
            openList.add(new Node(fScore[currentIndex], r, c));
            continue;
          }
        }
      }

      for (int k = 0; k < 4; k++) {
        int nr = r + dRow[k];
        int nc = c + dCol[k];

        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
          int neighborIndex = nr * cols + nc;

          if (closed[neighborIndex]) {
            continue;
          }

          MazeCell cell = grid.get(nr).get(nc);
          if (!cell.isWalkable()) {
            continue;
          }

          double stepCost = cell.stepCost();
          double tentativeG = gScore[currentIndex] + stepCost;

          if (tentativeG < gScore[neighborIndex]) {
            parent[neighborIndex] = currentIndex;
            gScore[neighborIndex] = tentativeG;
            double h = calculateHValue(nr, nc, goalRow, goalCol);
            fScore[neighborIndex] = tentativeG + h;
            openList.add(new Node(fScore[neighborIndex], nr, nc));
          }
        }
      }
      expansions++;
    }

    log("Theta* exhausted search after expanding " + expansions + " nodes with no path");
    throw new MazeSolvingException("No path found from start to goal");
  }

  private boolean hasLineOfSight(List<List<MazeCell>> grid, int rows, int cols,
      int r1, int c1, int r2, int c2) {
    int dr = r2 - r1;
    int dc = c2 - c1;
    int steps = Math.max(Math.abs(dr), Math.abs(dc));

    if (steps == 0) {
      return true;
    }

    double stepR = (double) dr / steps;
    double stepC = (double) dc / steps;

    for (int i = 1; i < steps; i++) {
      int r = (int) Math.round(r1 + i * stepR);
      int c = (int) Math.round(c1 + i * stepC);

      if (r < 0 || r >= rows || c < 0 || c >= cols) {
        return false;
      }

      if (!grid.get(r).get(c).isWalkable()) {
        return false;
      }
    }

    return true;
  }

  private double calculateDirectCost(List<List<MazeCell>> grid, int rows, int cols,
      int r1, int c1, int r2, int c2) {
    int dr = r2 - r1;
    int dc = c2 - c1;
    int steps = Math.max(Math.abs(dr), Math.abs(dc));

    if (steps == 0) {
      return 0.0;
    }

    double stepR = (double) dr / steps;
    double stepC = (double) dc / steps;
    double totalCost = 0.0;

    for (int i = 1; i <= steps; i++) {
      int r = (int) Math.round(r1 + i * stepR);
      int c = (int) Math.round(c1 + i * stepC);

      if (r >= 0 && r < rows && c >= 0 && c < cols) {
        totalCost += grid.get(r).get(c).stepCost();
      }
    }

    return totalCost;
  }

  private double calculateHValue(int r, int c, int gr, int gc) {
    return Math.abs(r - gr) + Math.abs(c - gc);
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
    log("Theta* reconstructed path of length " + path.size());
    return path;
  }

  private static class Node implements Comparable<Node> {
    double f;
    int r, c;

    Node(double f, int r, int c) {
      this.f = f;
      this.r = r;
      this.c = c;
    }

    @Override
    public int compareTo(Node other) {
      return Double.compare(this.f, other.f);
    }
  }
}
