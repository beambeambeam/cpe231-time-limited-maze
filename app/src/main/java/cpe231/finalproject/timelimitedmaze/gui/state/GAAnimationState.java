package cpe231.finalproject.timelimitedmaze.gui.state;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import java.util.List;
import java.util.Map;

public record GAAnimationState(
    Maze maze,
    boolean isPlaying,
    int currentGeneration,
    List<Coordinate> currentBestPath,
    double currentBestFitness,
    boolean goalReached,
    String selectedMazeFileName,
    boolean mazeDropdownOpen,
    List<String> availableMazeFiles,
    Map<String, Boolean> mazeValidityMap,
    int generationSpeedMs,
    boolean speedDropdownOpen,
    long lastGenerationTime,
    int cellSize,
    List<List<Coordinate>> allPopulationPaths) {
}
