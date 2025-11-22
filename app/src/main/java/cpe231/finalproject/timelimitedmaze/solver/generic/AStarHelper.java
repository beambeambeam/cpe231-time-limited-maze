package cpe231.finalproject.timelimitedmaze.solver.generic;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* pathfinding helper with depth limit for use in genetic algorithm
 * mutations.
 *
 * Used by the RegrowthMutation operator to bridge gaps in paths when segments
 * are deleted. The depth limit prevents excessive computation during mutation.
 */
public final class AStarHelper {

  private AStarHelper() {
  }

  /**
   * Finds a path between two coordinates using A* algorithm with depth limit.
   *
   * @param maze     The maze to navigate
   * @param from     Starting coordinate
   * @param to       Target coordinate
   * @param maxDepth Maximum depth to search (prevents excessive computation)
   * @return Path from 'from' to 'to', or empty list if not found within depth
   *         limit
   */
  public static List<Coordinate> findPath(Maze maze, Coordinate from, Coordinate to, int maxDepth) {
    if (from.equals(to)) {
      return List.of(from);
    }

    PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(Node::getF));
    Set<Coordinate> closedSet = new HashSet<>();
    Map<Coordinate, Coordinate> cameFrom = new HashMap<>();
    Map<Coordinate, Integer> gScore = new HashMap<>();

    gScore.put(from, 0);
    openSet.add(new Node(from, 0, manhattanDistance(from, to)));

    while (!openSet.isEmpty()) {
      Node current = openSet.poll();
      Coordinate currentCoord = current.coordinate;

      if (currentCoord.equals(to)) {
        return reconstructPath(cameFrom, currentCoord);
      }

      if (gScore.getOrDefault(currentCoord, Integer.MAX_VALUE) > maxDepth) {
        continue;
      }

      closedSet.add(currentCoord);

      for (Coordinate neighbor : getWalkableNeighbors(maze, currentCoord)) {
        if (closedSet.contains(neighbor)) {
          continue;
        }

        int tentativeG = gScore.getOrDefault(currentCoord, Integer.MAX_VALUE) +
            getStepCost(maze, neighbor);

        if (tentativeG > maxDepth) {
          continue;
        }

        if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
          cameFrom.put(neighbor, currentCoord);
          gScore.put(neighbor, tentativeG);
          int h = manhattanDistance(neighbor, to);
          openSet.add(new Node(neighbor, tentativeG, h));
        }
      }
    }

    return new ArrayList<>();
  }

  private static List<Coordinate> reconstructPath(Map<Coordinate, Coordinate> cameFrom,
      Coordinate current) {
    List<Coordinate> path = new ArrayList<>();
    path.add(current);

    while (cameFrom.containsKey(current)) {
      current = cameFrom.get(current);
      path.addFirst(current);
    }

    return path;
  }

  private static List<Coordinate> getWalkableNeighbors(Maze maze, Coordinate coord) {
    List<Coordinate> neighbors = new ArrayList<>();

    int[][] deltas = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    for (int[] delta : deltas) {
      int newRow = coord.row() + delta[0];
      int newCol = coord.column() + delta[1];

      if (newRow >= 0 && newRow < maze.getHeight() &&
          newCol >= 0 && newCol < maze.getWidth()) {
        Coordinate neighbor = new Coordinate(newRow, newCol);
        if (maze.getCell(neighbor).isWalkable()) {
          neighbors.add(neighbor);
        }
      }
    }

    return neighbors;
  }

  private static int getStepCost(Maze maze, Coordinate coord) {
    return maze.getCell(coord).stepCost();
  }

  private static int manhattanDistance(Coordinate a, Coordinate b) {
    return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column());
  }

  private static final class Node {
    private final Coordinate coordinate;
    private final int g;
    private final int h;

    Node(Coordinate coordinate, int g, int h) {
      this.coordinate = coordinate;
      this.g = g;
      this.h = h;
    }

    int getF() {
      return g + h;
    }
  }
}
