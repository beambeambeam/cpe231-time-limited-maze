package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class BidirectionalDijkstraSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "Bidirectional Dijkstra";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("Bidirectional Dijkstra start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    double[] distForward = new double[size];
    double[] distBackward = new double[size];
    int[] parentForward = new int[size];
    int[] parentBackward = new int[size];
    boolean[] visitedForward = new boolean[size];
    boolean[] visitedBackward = new boolean[size];

    Arrays.fill(distForward, Double.POSITIVE_INFINITY);
    Arrays.fill(distBackward, Double.POSITIVE_INFINITY);
    Arrays.fill(parentForward, -1);
    Arrays.fill(parentBackward, -1);

    int startIndex = startRow * cols + startCol;
    int goalIndex = goalRow * cols + goalCol;

    distForward[startIndex] = 0.0;
    distBackward[goalIndex] = 0.0;

    PriorityQueue<Node> pqForward = new PriorityQueue<>();
    PriorityQueue<Node> pqBackward = new PriorityQueue<>();

    pqForward.add(new Node(0.0, startRow, startCol));
    pqBackward.add(new Node(0.0, goalRow, goalCol));

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int expansions = 0;
    double bestDist = Double.POSITIVE_INFINITY;
    int meetingPoint = -1;

    while (!pqForward.isEmpty() || !pqBackward.isEmpty()) {
      if (!pqForward.isEmpty()) {
        Node current = pqForward.poll();
        int r = current.r;
        int c = current.c;
        int currentIndex = r * cols + c;

        if (visitedForward[currentIndex]) {
          continue;
        }
        visitedForward[currentIndex] = true;

        if (visitedBackward[currentIndex]) {
          double totalDist = distForward[currentIndex] + distBackward[currentIndex];
          if (totalDist < bestDist) {
            bestDist = totalDist;
            meetingPoint = currentIndex;
          }
        }

        for (int k = 0; k < 4; k++) {
          int nr = r + dRow[k];
          int nc = c + dCol[k];

          if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
            int neighborIndex = nr * cols + nc;

            if (visitedForward[neighborIndex]) {
              continue;
            }

            MazeCell cell = grid.get(nr).get(nc);
            if (!cell.isWalkable()) {
              continue;
            }

            double stepCost = cell.stepCost();
            double newDist = distForward[currentIndex] + stepCost;

            if (newDist < distForward[neighborIndex]) {
              distForward[neighborIndex] = newDist;
              parentForward[neighborIndex] = currentIndex;
              pqForward.add(new Node(newDist, nr, nc));
            }
          }
        }
        expansions++;
      }

      if (!pqBackward.isEmpty()) {
        Node current = pqBackward.poll();
        int r = current.r;
        int c = current.c;
        int currentIndex = r * cols + c;

        if (visitedBackward[currentIndex]) {
          continue;
        }
        visitedBackward[currentIndex] = true;

        if (visitedForward[currentIndex]) {
          double totalDist = distForward[currentIndex] + distBackward[currentIndex];
          if (totalDist < bestDist) {
            bestDist = totalDist;
            meetingPoint = currentIndex;
          }
        }

        for (int k = 0; k < 4; k++) {
          int nr = r + dRow[k];
          int nc = c + dCol[k];

          if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
            int neighborIndex = nr * cols + nc;

            if (visitedBackward[neighborIndex]) {
              continue;
            }

            MazeCell cell = grid.get(nr).get(nc);
            if (!cell.isWalkable()) {
              continue;
            }

            double stepCost = cell.stepCost();
            double newDist = distBackward[currentIndex] + stepCost;

            if (newDist < distBackward[neighborIndex]) {
              distBackward[neighborIndex] = newDist;
              parentBackward[neighborIndex] = currentIndex;
              pqBackward.add(new Node(newDist, nr, nc));
            }
          }
        }
        expansions++;
      }

      if (meetingPoint != -1 && bestDist < Double.POSITIVE_INFINITY) {
        return mergePaths(parentForward, parentBackward, meetingPoint, cols);
      }
    }

    log("Bidirectional Dijkstra exhausted search after expanding " + expansions + " nodes with no path");
    throw new MazeSolvingException("No path found from start to goal");
  }

  private List<Coordinate> mergePaths(int[] parentForward, int[] parentBackward, int meetingIndex, int cols) {
    List<Coordinate> forwardPath = new ArrayList<>();
    List<Coordinate> backwardPath = new ArrayList<>();

    int currIndex = meetingIndex;
    while (currIndex != -1) {
      int r = currIndex / cols;
      int c = currIndex % cols;
      forwardPath.add(new Coordinate(r, c));
      currIndex = parentForward[currIndex];
    }
    Collections.reverse(forwardPath);

    currIndex = parentBackward[meetingIndex];
    while (currIndex != -1) {
      int r = currIndex / cols;
      int c = currIndex % cols;
      backwardPath.add(new Coordinate(r, c));
      currIndex = parentBackward[currIndex];
    }

    List<Coordinate> fullPath = new ArrayList<>(forwardPath);
    fullPath.addAll(backwardPath);
    log("Bidirectional Dijkstra reconstructed path of length " + fullPath.size());
    return fullPath;
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

