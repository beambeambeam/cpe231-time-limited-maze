package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeadEndFillSolver extends MazeSolver {

  @Override
  public String getAlgorithmName() {
    return "Dead-End Fill";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    int height = maze.getHeight();
    int width = maze.getWidth();
    Coordinate start = maze.getStart();
    Coordinate goal = maze.getGoal();

    boolean[][] open = initializeWalkableGrid(maze, height, width);
    ensureEndpointsRemainOpen(open, start, goal);

    pruneDeadEnds(open, start, goal);
    return buildPathThroughOpenCells(open, start, goal);
  }

  private boolean[][] initializeWalkableGrid(Maze maze, int height, int width) {
    boolean[][] open = new boolean[height][width];
    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        Coordinate coordinate = new Coordinate(row, column);
        open[row][column] = isWalkable(maze, coordinate);
      }
    }
    return open;
  }

  private void ensureEndpointsRemainOpen(boolean[][] open, Coordinate start, Coordinate goal) {
    open[start.row()][start.column()] = true;
    open[goal.row()][goal.column()] = true;
  }

  private void pruneDeadEnds(boolean[][] open, Coordinate start, Coordinate goal) {
    int height = open.length;
    int width = open[0].length;
    Deque<Coordinate> candidates = new ArrayDeque<>();

    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        Coordinate coordinate = new Coordinate(row, column);
        if (isCandidateDeadEnd(open, coordinate, start, goal)) {
          candidates.add(coordinate);
        }
      }
    }

    while (!candidates.isEmpty()) {
      Coordinate current = candidates.removeFirst();
      if (current.equals(start) || current.equals(goal)) {
        continue;
      }
      if (!open[current.row()][current.column()]) {
        continue;
      }

      if (countOpenNeighbors(open, current) <= 1) {
        open[current.row()][current.column()] = false;
        for (Direction direction : Direction.values()) {
          Coordinate neighbor = move(current, direction);
          if (isWithinGrid(neighbor, height, width)
              && isCandidateDeadEnd(open, neighbor, start, goal)) {
            candidates.add(neighbor);
          }
        }
      }
    }
  }

  private boolean isCandidateDeadEnd(boolean[][] open, Coordinate coordinate,
                                     Coordinate start, Coordinate goal) {
    if (!isWithinGrid(coordinate, open.length, open[0].length)) {
      return false;
    }
    if (!open[coordinate.row()][coordinate.column()]) {
      return false;
    }
    if (coordinate.equals(start) || coordinate.equals(goal)) {
      return false;
    }
    return countOpenNeighbors(open, coordinate) <= 1;
  }

  private List<Coordinate> buildPathThroughOpenCells(boolean[][] open,
                                                     Coordinate start,
                                                     Coordinate goal) {
    int height = open.length;
    int width = open[0].length;
    boolean[][] visited = new boolean[height][width];
    Deque<Coordinate> queue = new ArrayDeque<>();
    Map<Coordinate, Coordinate> parent = new HashMap<>();

    queue.add(start);
    visited[start.row()][start.column()] = true;

    while (!queue.isEmpty()) {
      Coordinate current = queue.removeFirst();
      if (current.equals(goal)) {
        return reconstructPath(parent, start, goal);
      }

      for (Direction direction : Direction.values()) {
        Coordinate neighbor = move(current, direction);
        if (isWithinGrid(neighbor, height, width)
            && open[neighbor.row()][neighbor.column()]
            && !visited[neighbor.row()][neighbor.column()]) {
          visited[neighbor.row()][neighbor.column()] = true;
          parent.put(neighbor, current);
          queue.add(neighbor);
        }
      }
    }

    throw new MazeSolvingException("Dead-end fill could not find a path to the goal");
  }

  private List<Coordinate> reconstructPath(Map<Coordinate, Coordinate> parent,
                                           Coordinate start,
                                           Coordinate goal) {
    List<Coordinate> path = new ArrayList<>();
    Coordinate current = goal;
    while (current != null) {
      path.add(current);
      current = parent.get(current);
    }
    if (!path.get(path.size() - 1).equals(start)) {
      throw new MazeSolvingException("No path connects start to goal after filling dead ends");
    }
    List<Coordinate> orderedPath = new ArrayList<>(path.size());
    for (int index = path.size() - 1; index >= 0; index--) {
      orderedPath.add(path.get(index));
    }
    return orderedPath;
  }

  private int countOpenNeighbors(boolean[][] open, Coordinate coordinate) {
    int count = 0;
    for (Direction direction : Direction.values()) {
      Coordinate neighbor = move(coordinate, direction);
      if (isWithinGrid(neighbor, open.length, open[0].length)
          && open[neighbor.row()][neighbor.column()]) {
        count++;
      }
    }
    return count;
  }

  private boolean isWithinGrid(Coordinate coordinate, int height, int width) {
    return coordinate.row() >= 0
        && coordinate.row() < height
        && coordinate.column() >= 0
        && coordinate.column() < width;
  }
}
