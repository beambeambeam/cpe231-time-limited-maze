package cpe231.finalproject.timelimitedmaze.utils;

import cpe231.finalproject.timelimitedmaze.solver.GeneticAlgorithmSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.WallFollowerSolver;

import java.util.List;

public final class SolverRegistry {
  private SolverRegistry() {
  }

  public static List<MazeSolver> getAvailableSolvers() {
    return List.of(
        new WallFollowerSolver(WallFollowerSolver.WallSide.LEFT),
        new WallFollowerSolver(WallFollowerSolver.WallSide.RIGHT),
        new GeneticAlgorithmSolver());
  }
}
