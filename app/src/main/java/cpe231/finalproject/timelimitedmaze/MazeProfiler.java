package cpe231.finalproject.timelimitedmaze;

import cpe231.finalproject.timelimitedmaze.solver.MazeSolver;
import cpe231.finalproject.timelimitedmaze.solver.SolverResult;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeFileLister;
import cpe231.finalproject.timelimitedmaze.utils.MazeStore;
import cpe231.finalproject.timelimitedmaze.utils.SolverRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Profiles maze solvers on available mazes.
 *
 * Runs selected algorithms on selected mazes and displays results in a table
 * format.
 * Supports filtering by algorithm and maze names.
 */
public final class MazeProfiler {

  private MazeProfiler() {
  }

  public static void main(String[] args) {
    if (args.length > 0 && (args[0].equals("--list") || args[0].equals("-l"))) {
      printAvailableOptions();
      return;
    }

    ParsedArgs parsed = parseArguments(args);
    if (parsed.showHelp()) {
      printUsage();
      return;
    }

    List<MazeSolver> allSolvers = SolverRegistry.getAvailableSolvers();
    List<String> allMazeFiles = MazeFileLister.listMazeFiles();

    List<MazeSolver> selectedSolvers = filterSolvers(allSolvers, parsed.algorithms());
    List<String> selectedMazeFiles = filterMazeFiles(allMazeFiles, parsed.mazes());

    if (selectedSolvers.isEmpty()) {
      System.err.println("Error: No valid algorithms selected.");
      System.err.println("Available algorithms: " + allSolvers.stream()
          .map(MazeSolver::getAlgorithmName)
          .toList());
      return;
    }

    if (selectedMazeFiles.isEmpty()) {
      System.err.println("Error: No valid mazes selected.");
      System.err.println("Available mazes: " + allMazeFiles);
      return;
    }

    System.out.println("=== Maze Solver Profiler ===\n");
    System.out
        .println("Running " + selectedSolvers.size() + " solver(s) on " + selectedMazeFiles.size() + " maze(s)...\n");

    List<ProfileResult> results = new ArrayList<>();

    for (MazeSolver solver : selectedSolvers) {
      String solverName = solver.getAlgorithmName();
      System.out.println("Testing: " + solverName);

      for (String mazeFile : selectedMazeFiles) {
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

    printResultsTable(results, selectedSolvers, selectedMazeFiles);
  }

  private static void printAvailableOptions() {
    System.out.println("=== Available Options ===\n");

    System.out.println("Algorithms:");
    List<MazeSolver> solvers = SolverRegistry.getAvailableSolvers();
    for (MazeSolver solver : solvers) {
      System.out.println("  - " + solver.getAlgorithmName());
    }

    System.out.println("\nMazes:");
    List<String> mazeFiles = MazeFileLister.listMazeFiles();
    for (String mazeFile : mazeFiles) {
      System.out.println("  - " + mazeFile);
    }
  }

  private static void printUsage() {
    System.out.println("Usage: MazeProfiler [options]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  -a, --algo <name>     Select algorithm(s) to profile (can specify multiple)");
    System.out.println("  -m, --maze <name>    Select maze(s) to profile (can specify multiple)");
    System.out.println("  -l, --list           List available algorithms and mazes");
    System.out.println("  -h, --help           Show this help message");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  ./gradlew :app:profiler -Pargs=\"--algo 'Genetic Algorithm' --maze m15_15.txt\"");
    System.out.println(
        "  ./gradlew :app:profiler -Pargs=\"-a 'Wall Follower (LEFT)' -a 'Wall Follower (RIGHT)' -m m30_30.txt\"");
    System.out.println("  ./gradlew :app:profiler -Pargs=\"--list\"");
  }

  private static ParsedArgs parseArguments(String[] args) {
    Set<String> algorithms = new HashSet<>();
    Set<String> mazes = new HashSet<>();
    boolean showHelp = false;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("--help") || arg.equals("-h")) {
        showHelp = true;
      } else if (arg.equals("--algo") || arg.equals("-a")) {
        if (i + 1 < args.length) {
          String value = args[++i];
          value = value.replaceAll("^['\"]|['\"]$", "");
          algorithms.add(value);
        }
      } else if (arg.equals("--maze") || arg.equals("-m")) {
        if (i + 1 < args.length) {
          String value = args[++i];
          value = value.replaceAll("^['\"]|['\"]$", "");
          mazes.add(value);
        }
      }
    }

    return new ParsedArgs(algorithms.isEmpty() ? null : algorithms, mazes.isEmpty() ? null : mazes, showHelp);
  }

  private static List<MazeSolver> filterSolvers(List<MazeSolver> allSolvers, Set<String> selectedNames) {
    if (selectedNames == null) {
      return allSolvers;
    }

    List<MazeSolver> filtered = new ArrayList<>();
    for (MazeSolver solver : allSolvers) {
      if (selectedNames.contains(solver.getAlgorithmName())) {
        filtered.add(solver);
      }
    }
    return filtered;
  }

  private static List<String> filterMazeFiles(List<String> allMazeFiles, Set<String> selectedNames) {
    if (selectedNames == null) {
      return allMazeFiles;
    }

    List<String> filtered = new ArrayList<>();
    for (String mazeFile : allMazeFiles) {
      if (selectedNames.contains(mazeFile)) {
        filtered.add(mazeFile);
      }
    }
    return filtered;
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

  private record ParsedArgs(Set<String> algorithms, Set<String> mazes, boolean showHelp) {
  }
}
