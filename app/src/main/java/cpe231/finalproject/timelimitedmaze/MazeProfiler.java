package cpe231.finalproject.timelimitedmaze;

import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import cpe231.finalproject.timelimitedmaze.utils.SolverRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Profiles all maze solvers on all available mazes.
 *
 * Runs each algorithm on each maze and displays results in a table format.
 */
public final class MazeProfiler {

  private MazeProfiler() {
  }

  public static void main(String[] args) {
    System.out.println("=== Maze Solver Profiler ===\n");

    List<MazeSolver> solvers = SolverRegistry.getAvailableSolvers();
    List<String> mazeFiles = MazeFileLister.listMazeFiles();

    List<ProfileResult> results = new ArrayList<>();

    System.out.println("Running " + solvers.size() + " solver(s) on " + mazeFiles.size() + " maze(s)...\n");

    for (MazeSolver solver : solvers) {
      String solverName = solver.getAlgorithmName();
      System.out.println("Testing: " + solverName);

      for (String mazeFile : mazeFiles) {
        try {
          Maze maze = MazeStore.getMaze(mazeFile);
          System.out.print("  " + mazeFile + "... ");

          SolverResult result = solver.solve(maze);

          long executionTimeNs = result.endTimeNs() - result.startTimeNs();
          double executionTimeMs = executionTimeNs / 1_000_000.0;
          boolean reachedGoal = result.path().getLast().equals(maze.getGoal());

          results.add(new ProfileResult(solverName, mazeFile, result.totalCost(),
              result.path().size(), executionTimeMs, reachedGoal));

          System.out.println(reachedGoal ? "✓" : "✗");

        } catch (Exception e) {
          System.out.println("ERROR: " + e.getMessage());
          results.add(new ProfileResult(solverName, mazeFile, -1, -1, -1.0, false));
        }
      }
      System.out.println();
    }

    printResultsTable(results, solvers, mazeFiles);
  }

  private static void printResultsTable(List<ProfileResult> results,
      List<MazeSolver> solvers, List<String> mazeFiles) {
    System.out.println("\n=== Results Table ===\n");

    int solverNameWidth = Math.max(20, solvers.stream()
        .mapToInt(s -> s.getAlgorithmName().length())
        .max().orElse(20));
    int mazeNameWidth = Math.max(15, mazeFiles.stream()
        .mapToInt(String::length)
        .max().orElse(15));

    String headerFormat = "%-" + solverNameWidth + "s | %-" + mazeNameWidth + "s | %8s | %8s | %12s | %6s";
    String rowFormat = "%-" + solverNameWidth + "s | %-" + mazeNameWidth + "s | %8d | %8d | %12.3f ms | %6s";

    System.out.printf(headerFormat, "Algorithm", "Maze", "Cost", "Length", "Time", "Goal");
    System.out.println();
    System.out.println("-".repeat(solverNameWidth + mazeNameWidth + 55));

    for (ProfileResult result : results) {
      String goalStatus = result.reachedGoal() ? "✓" : "✗";
      if (result.cost() < 0) {
        System.out.printf("%-" + solverNameWidth + "s | %-" + mazeNameWidth + "s | %8s | %8s | %12s | %6s",
            result.solverName(), result.mazeFile(), "ERROR", "ERROR", "ERROR", "ERROR");
      } else {
        System.out.printf(rowFormat, result.solverName(), result.mazeFile(),
            result.cost(), result.pathLength(), result.timeMs(), goalStatus);
      }
      System.out.println();
    }

    System.out.println("\n=== Summary ===\n");

    for (MazeSolver solver : solvers) {
      String solverName = solver.getAlgorithmName();
      List<ProfileResult> solverResults = results.stream()
          .filter(r -> r.solverName().equals(solverName))
          .toList();

      double totalTime = solverResults.stream()
          .filter(r -> r.timeMs() >= 0.0)
          .mapToDouble(ProfileResult::timeMs)
          .sum();
      int successCount = (int) solverResults.stream()
          .filter(ProfileResult::reachedGoal)
          .count();
      int totalCost = solverResults.stream()
          .filter(r -> r.cost() >= 0)
          .mapToInt(ProfileResult::cost)
          .sum();

      System.out.println(solverName + ":");
      System.out.println("  Success rate: " + successCount + "/" + solverResults.size());
      System.out.printf("  Total time: %.3f ms%n", totalTime);
      System.out.println("  Average cost: " + (solverResults.size() > 0 ? totalCost / solverResults.size() : 0));
      System.out.println();
    }
  }

  private record ProfileResult(String solverName, String mazeFile, int cost,
      int pathLength, double timeMs, boolean reachedGoal) {
  }
}
