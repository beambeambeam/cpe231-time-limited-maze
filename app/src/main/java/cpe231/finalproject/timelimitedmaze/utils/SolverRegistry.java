package cpe231.finalproject.timelimitedmaze.utils;

import cpe231.finalproject.timelimitedmaze.solver.AstarSolver;
import cpe231.finalproject.timelimitedmaze.solver.BellmanFordSolver;
import cpe231.finalproject.timelimitedmaze.solver.BestFirstSolver;
import cpe231.finalproject.timelimitedmaze.solver.BFSSolver;
import cpe231.finalproject.timelimitedmaze.solver.BidirectionalBFSSolver;
import cpe231.finalproject.timelimitedmaze.solver.BidirectionalDijkstraSolver;
import cpe231.finalproject.timelimitedmaze.solver.DeadEndFillSolver;
import cpe231.finalproject.timelimitedmaze.solver.DFSSolver;
import cpe231.finalproject.timelimitedmaze.solver.DijkstraSolver;
import cpe231.finalproject.timelimitedmaze.solver.GeneticAlgorithmSolver;
import cpe231.finalproject.timelimitedmaze.solver.IDDFSSolver;
import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.SPFASolver;
import cpe231.finalproject.timelimitedmaze.solver.ThetaStarSolver;
import cpe231.finalproject.timelimitedmaze.solver.WallFollowerSolver;
import cpe231.finalproject.timelimitedmaze.solver.WeightedAStarSolver;

import java.util.List;

public final class SolverRegistry {
  private SolverRegistry() {
  }

  public static List<MazeSolver> getAvailableSolvers() {
    return List.of(
        new WallFollowerSolver(WallFollowerSolver.WallSide.LEFT),
        new WallFollowerSolver(WallFollowerSolver.WallSide.RIGHT),
        new DeadEndFillSolver(),
        new GeneticAlgorithmSolver(),
        new AstarSolver(),
        new DijkstraSolver(),
        new BFSSolver(),
        new BidirectionalBFSSolver(),
        new BestFirstSolver(),
        new DFSSolver(),
        new IDDFSSolver(),
        new BidirectionalDijkstraSolver(),
        new WeightedAStarSolver(),
        new BellmanFordSolver(),
        new SPFASolver(),
        new ThetaStarSolver());
  }
}
