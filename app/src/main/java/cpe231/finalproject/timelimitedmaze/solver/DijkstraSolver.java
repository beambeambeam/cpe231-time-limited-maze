package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class DijkstraSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "Dijkstra's Algorithm";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("Dijkstra start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    double[] dist = new double[size];
    int[] parent = new int[size];
    boolean[] visited = new boolean[size];

    Arrays.fill(dist, Double.POSITIVE_INFINITY);
    Arrays.fill(parent, -1);

    int startIndex = startRow * cols + startCol;
    dist[startIndex] = 0.0;

    PriorityQueue<Node> pq = new PriorityQueue<>();
    pq.add(new Node(0.0, startRow, startCol));

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int expansions = 0;

    while (!pq.isEmpty()) {
      Node current = pq.poll();
      int r = current.r;
      int c = current.c;
      int currentIndex = r * cols + c;

      if (visited[currentIndex]) {
        continue;
      }
      visited[currentIndex] = true;

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

          double stepCost = cell.stepCost();
          double newDist = dist[currentIndex] + stepCost;

          if (newDist < dist[neighborIndex]) {
            dist[neighborIndex] = newDist;
            parent[neighborIndex] = currentIndex;
            pq.add(new Node(newDist, nr, nc));
          }
        }
      }
      expansions++;
    }

    log("Dijkstra exhausted search after expanding " + expansions + " nodes with no path");
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
    log("Dijkstra reconstructed path of length " + path.size());
    return path;
  }

  private static class Node implements Comparable<Node> {
    double dist;
    int r, c;

    Node(double dist, int r, int c) {
      this.dist = dist;
      this.r = r;
      this.c = c;
    }

    @Override
    public int compareTo(Node other) {
      return Double.compare(this.dist, other.dist);
    }
  }
}

