package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public final class BidirectionalBFSSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "Bidirectional BFS";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int rows = maze.getHeight();
    int cols = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();
    log("Bidirectional BFS start: " + start + " -> goal: " + goal + " grid " + rows + "x" + cols);

    int startRow = start.row();
    int startCol = start.column();
    int goalRow = goal.row();
    int goalCol = goal.column();

    int size = rows * cols;
    int[] parentForward = new int[size];
    int[] parentBackward = new int[size];
    boolean[] visitedForward = new boolean[size];
    boolean[] visitedBackward = new boolean[size];

    for (int i = 0; i < size; i++) {
      parentForward[i] = -1;
      parentBackward[i] = -1;
    }

    int startIndex = startRow * cols + startCol;
    int goalIndex = goalRow * cols + goalCol;

    visitedForward[startIndex] = true;
    visitedBackward[goalIndex] = true;

    Queue<Node> queueForward = new ArrayDeque<>();
    Queue<Node> queueBackward = new ArrayDeque<>();

    queueForward.add(new Node(startRow, startCol));
    queueBackward.add(new Node(goalRow, goalCol));

    List<List<MazeCell>> grid = maze.getGrid();

    int[] dRow = {-1, 1, 0, 0};
    int[] dCol = {0, 0, 1, -1};

    int expansions = 0;

    while (!queueForward.isEmpty() || !queueBackward.isEmpty()) {
      if (!queueForward.isEmpty()) {
        Node current = queueForward.poll();
        int r = current.r;
        int c = current.c;
        int currentIndex = r * cols + c;

        if (visitedBackward[currentIndex]) {
          return mergePaths(parentForward, parentBackward, currentIndex, cols);
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

            visitedForward[neighborIndex] = true;
            parentForward[neighborIndex] = currentIndex;
            queueForward.add(new Node(nr, nc));
          }
        }
        expansions++;
      }

      if (!queueBackward.isEmpty()) {
        Node current = queueBackward.poll();
        int r = current.r;
        int c = current.c;
        int currentIndex = r * cols + c;

        if (visitedForward[currentIndex]) {
          return mergePaths(parentForward, parentBackward, currentIndex, cols);
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

            visitedBackward[neighborIndex] = true;
            parentBackward[neighborIndex] = currentIndex;
            queueBackward.add(new Node(nr, nc));
          }
        }
        expansions++;
      }
    }

    log("Bidirectional BFS exhausted search after expanding " + expansions + " nodes with no path");
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
    log("Bidirectional BFS reconstructed path of length " + fullPath.size());
    return fullPath;
  }

  private static class Node {
    int r, c;

    Node(int r, int c) {
      this.r = r;
      this.c = c;
    }
  }
}

