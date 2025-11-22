package cpe231.finalproject.timelimitedmaze;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.solver.generic.GeneticAlgorithmSolver;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-computes solutions for the genetic algorithm solver on all available
 * mazes.
 *
 * Runs the genetic algorithm once on each maze to find and cache the best
 * solutions.
 * The GA will evolve through multiple generations internally until it finds a
 * solution or reaches termination criteria.
 */
public final class GAPrecompute {

  private GAPrecompute() {
  }

  public static void main(String[] args) {
    System.out.println("=== Genetic Algorithm Pre-computation ===\n");

    List<String> mazeFiles = MazeFileLister.listMazeFiles();
    List<PrecomputeResult> results = new ArrayList<>();

    System.out.println("Pre-computing solutions for " + mazeFiles.size() + " maze(s)...\n");

    GeneticAlgorithmSolver solver = new GeneticAlgorithmSolver();

    for (String mazeFile : mazeFiles) {
      try {
        Maze maze = MazeStore.getMaze(mazeFile);
        System.out.print("  " + mazeFile + "... ");

        SolverResult result = solver.solve(maze);

        long executionTimeNs = result.endTimeNs() - result.startTimeNs();
        double executionTimeMs = executionTimeNs / 1_000_000.0;
        boolean reachedGoal = result.path().getLast().equals(maze.getGoal());

        results.add(new PrecomputeResult(mazeFile, result.totalCost(),
            result.path().size(), executionTimeMs, reachedGoal));

        System.out.println(reachedGoal ? "✓" : "✗");

      } catch (Exception e) {
        System.out.println("ERROR: " + e.getMessage());
        results.add(new PrecomputeResult(mazeFile, -1, -1, -1.0, false));
      }
    }

    printResultsTable(results, mazeFiles);
  }

  private static void printResultsTable(List<PrecomputeResult> results,
      List<String> mazeFiles) {
    System.out.println("\n=== Results Table ===\n");

    int mazeNameWidth = Math.max(15, mazeFiles.stream()
        .mapToInt(String::length)
        .max().orElse(15));

    String headerFormat = "%-" + mazeNameWidth + "s | %8s | %8s | %12s | %6s";
    String rowFormat = "%-" + mazeNameWidth + "s | %8d | %8d | %12.3f ms | %6s";

    System.out.printf(headerFormat, "Maze", "Cost", "Length", "Time", "Goal");
    System.out.println();
    System.out.println("-".repeat(mazeNameWidth + 45));

    for (PrecomputeResult result : results) {
      String goalStatus = result.reachedGoal() ? "✓" : "✗";
      if (result.cost() < 0) {
        System.out.printf("%-" + mazeNameWidth + "s | %8s | %8s | %12s | %6s",
            result.mazeFile(), "ERROR", "ERROR", "ERROR", "ERROR");
      } else {
        System.out.printf(rowFormat, result.mazeFile(),
            result.cost(), result.pathLength(), result.timeMs(), goalStatus);
      }
      System.out.println();
    }

    System.out.println("\n=== Summary ===\n");

    int successCount = (int) results.stream()
        .filter(PrecomputeResult::reachedGoal)
        .count();
    double totalTime = results.stream()
        .filter(r -> r.timeMs() >= 0.0)
        .mapToDouble(PrecomputeResult::timeMs)
        .sum();
    int totalCost = results.stream()
        .filter(r -> r.cost() >= 0)
        .mapToInt(PrecomputeResult::cost)
        .sum();

    System.out.println("Success rate: " + successCount + "/" + results.size());
    System.out.printf("Total time: %.3f ms%n", totalTime);
    System.out.println("Average cost: " + (results.size() > 0 ? totalCost / results.size() : 0));
    System.out.println();
    System.out.println("All solutions cached in: ga_checkpoints/");
    System.out.println("You can now use the Genetic Algorithm solver in the GUI");
  }

  private record PrecomputeResult(String mazeFile, int cost,
      int pathLength, double timeMs, boolean reachedGoal) {
  }
}
