package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.ArrayList;
import java.util.List;

public final class WallFollowerSolver extends MazeSolver {

  public enum WallSide {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
  }

  private static final int STEP_MULTIPLIER = 10;
  private final WallSide wallSide;

  public WallFollowerSolver(WallSide wallSide) {
    this.wallSide = wallSide;
  }

  @Override
  public String getAlgorithmName() {
    return "Wall Follower (" + wallSide.name() + ")";
  }

  @Override
  protected List<Coordinate> executeSolve(Maze maze) {
    Coordinate current = maze.getStart();
    Direction direction = findInitialDirection(maze, current);
    List<Coordinate> path = new ArrayList<>();
    path.add(current);

    if (current.equals(maze.getGoal())) {
      return path;
    }

    int maxSteps = Math.max(maze.getWidth() * maze.getHeight() * STEP_MULTIPLIER, 1);

    for (int step = 0; step < maxSteps; step++) {
      Direction preferredDirection = getPreferredDirection(direction);
      Coordinate preferredCoordinate = move(current, preferredDirection);
      Coordinate forwardCoordinate = move(current, direction);
      Direction oppositePreferredDirection = getOppositePreferredDirection(direction);
      Coordinate oppositePreferredCoordinate = move(current, oppositePreferredDirection);

      if (isWalkable(maze, preferredCoordinate)) {
        direction = preferredDirection;
        current = preferredCoordinate;
        path.add(current);
      } else if (isWalkable(maze, forwardCoordinate)) {
        current = forwardCoordinate;
        path.add(current);
      } else if (isWalkable(maze, oppositePreferredCoordinate)) {
        direction = oppositePreferredDirection;
        current = oppositePreferredCoordinate;
        path.add(current);
      } else {
        Direction backwardsDirection = direction.opposite();
        Coordinate backwards = move(current, backwardsDirection);
        if (!isWalkable(maze, backwards)) {
          throw new MazeSolvingException("Solver is trapped and cannot move");
        }
        direction = backwardsDirection;
        current = backwards;
        path.add(current);
      }

      if (current.equals(maze.getGoal())) {
        return path;
      }
    }

    throw new MazeSolvingException("Wall follower failed to reach the goal within step limit");
  }

  private Direction getPreferredDirection(Direction currentDirection) {
    return switch (wallSide) {
      case LEFT -> currentDirection.left();
      case RIGHT -> currentDirection.right();
      case TOP -> getTopDirection(currentDirection);
      case BOTTOM -> getBottomDirection(currentDirection);
    };
  }

  private Direction getOppositePreferredDirection(Direction currentDirection) {
    return switch (wallSide) {
      case LEFT -> currentDirection.right();
      case RIGHT -> currentDirection.left();
      case TOP -> getBottomDirection(currentDirection);
      case BOTTOM -> getTopDirection(currentDirection);
    };
  }

  private Direction getTopDirection(Direction currentDirection) {
    return switch (currentDirection) {
      case NORTH -> Direction.WEST;
      case EAST -> Direction.NORTH;
      case SOUTH -> Direction.EAST;
      case WEST -> Direction.SOUTH;
    };
  }

  private Direction getBottomDirection(Direction currentDirection) {
    return switch (currentDirection) {
      case NORTH -> Direction.EAST;
      case EAST -> Direction.SOUTH;
      case SOUTH -> Direction.WEST;
      case WEST -> Direction.NORTH;
    };
  }

  private Direction findInitialDirection(Maze maze, Coordinate start) {
    for (Direction direction : Direction.values()) {
      if (isWalkable(maze, move(start, direction))) {
        return direction;
      }
    }
    throw new MazeSolvingException("Starting position is enclosed by walls");
  }
}
