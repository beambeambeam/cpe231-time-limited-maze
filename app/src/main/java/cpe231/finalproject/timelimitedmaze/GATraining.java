package cpe231.finalproject.timelimitedmaze;

import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.solver.generic.GeneticAlgorithmSolver;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Training runner for the genetic algorithm solver.
 *
 * Runs the genetic algorithm iteratively on all available mazes to generate and
 * improve cached solutions.
 * This allows the solver to use cached solutions instead of running from
 * scratch.
 */
public final class GATraining {

  private static final int DEFAULT_ITERATIONS = 10;

  private GATraining() {
  }

  public static void main(String[] args) {
    String mazeName = "m15_15.txt";
    int iterations = DEFAULT_ITERATIONS;

    if (args.length > 0) {
      String firstArg = args[0];
      if ("random".equalsIgnoreCase(firstArg)) {
        List<String> allMazes = MazeFileLister.listMazeFiles();
        mazeName = allMazes.get(new Random().nextInt(allMazes.size()));
        System.out.println("Selected random maze: " + mazeName);
      } else if (firstArg.endsWith(".txt")) {
        mazeName = firstArg;
      } else {
        try {
          iterations = Integer.parseInt(firstArg);
          if (iterations < 1) {
            iterations = DEFAULT_ITERATIONS;
          }
        } catch (NumberFormatException e) {
          System.out.println("Invalid argument, using defaults");
        }
      }
    }

    if (args.length > 1) {
      try {
        iterations = Integer.parseInt(args[1]);
        if (iterations < 1) {
          iterations = DEFAULT_ITERATIONS;
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid iteration count, using default: " + DEFAULT_ITERATIONS);
      }
    }

    System.out.println("=== Genetic Algorithm Training ===");
    System.out.println("Training on " + mazeName + " for " + iterations + " iterations...");
    System.out.println();

    List<String> mazeFiles = List.of(mazeName);
    Map<String, Integer> bestCosts = new HashMap<>();
    Map<String, Integer> bestPathLengths = new HashMap<>();
    Map<String, Boolean> reachedGoals = new HashMap<>();

    for (int iteration = 1; iteration <= iterations; iteration++) {
      System.out.println("=== Iteration " + iteration + "/" + iterations + " ===");

      int iterationSuccessCount = 0;
      int iterationTotalCost = 0;
      long iterationTotalTimeNs = 0;
      int improvedCount = 0;

      for (String mazeFile : mazeFiles) {
        System.out.print("  [" + iteration + "/" + iterations + "] " + mazeFile + "... ");

        try {
          Maze maze = MazeStore.getMaze(mazeFile);
          GeneticAlgorithmSolver freshSolver = new GeneticAlgorithmSolver(300, 100, new java.util.Random(), true);
          SolverResult result = freshSolver.solve(maze);

          long timeMs = (result.endTimeNs() - result.startTimeNs()) / 1_000_000;
          iterationTotalTimeNs += (result.endTimeNs() - result.startTimeNs());
          iterationTotalCost += result.totalCost();
          iterationSuccessCount++;

          boolean reachedGoal = result.path().getLast().equals(maze.getGoal());
          int currentCost = result.totalCost();
          int currentPathLength = result.path().size();

          boolean improved = false;
          if (!bestCosts.containsKey(mazeFile) ||
              (reachedGoal && (!reachedGoals.getOrDefault(mazeFile, false) || currentCost < bestCosts.get(mazeFile))) ||
              (!reachedGoal && !reachedGoals.getOrDefault(mazeFile, false)
                  && currentCost < bestCosts.getOrDefault(mazeFile, Integer.MAX_VALUE))) {
            bestCosts.put(mazeFile, currentCost);
            bestPathLengths.put(mazeFile, currentPathLength);
            reachedGoals.put(mazeFile, reachedGoal);
            improved = true;
            improvedCount++;
          }

          System.out.println(reachedGoal ? "GOAL ✓" : "PARTIAL");
          System.out.println("    Cost: " + currentCost +
              (bestCosts.containsKey(mazeFile) && currentCost == bestCosts.get(mazeFile) ? " (BEST)" : "") +
              (improved ? " (IMPROVED)" : ""));
          System.out.println("    Path length: " + currentPathLength);
          System.out.println("    Start: " + result.path().getFirst());
          System.out.println("    End: " + result.path().getLast());
          System.out.println("    Goal: " + maze.getGoal());
          int distToGoal = Math.abs(result.path().getLast().row() - maze.getGoal().row()) +
              Math.abs(result.path().getLast().column() - maze.getGoal().column());
          System.out.println("    Distance to goal: " + distToGoal);
          System.out.println("    Time: " + timeMs + " ms");

        } catch (Exception e) {
          System.out.println("FAILED: " + e.getMessage());
        }

        System.out.println();
      }

      System.out.println("--- Iteration " + iteration + " Summary ---");
      System.out.println("  Successful: " + iterationSuccessCount + "/" + mazeFiles.size());
      System.out.println("  Improved solutions: " + improvedCount);
      System.out
          .println("  Average cost: " + (iterationSuccessCount > 0 ? iterationTotalCost / iterationSuccessCount : 0));
      System.out.println("  Total time: " + (iterationTotalTimeNs / 1_000_000) + " ms");
      System.out.println();
    }

    System.out.println("=== Final Training Summary ===");
    int totalReachedGoal = 0;
    int totalBestCost = 0;
    for (String mazeFile : mazeFiles) {
      if (reachedGoals.getOrDefault(mazeFile, false)) {
        totalReachedGoal++;
      }
      totalBestCost += bestCosts.getOrDefault(mazeFile, 0);
    }

    System.out.println("Total iterations: " + iterations);
    System.out.println("Mazes processed: " + mazeFiles.size());
    System.out.println("Mazes reaching goal: " + totalReachedGoal);
    System.out.println("Average best cost: " + (mazeFiles.size() > 0 ? totalBestCost / mazeFiles.size() : 0));
    System.out.println();
    System.out.println("Best solutions cached in: ga_checkpoints/");
    System.out.println("You can now use the Genetic Algorithm solver in the GUI");
    System.out.println("and it will load cached solutions instead of re-running.");
  }
}
