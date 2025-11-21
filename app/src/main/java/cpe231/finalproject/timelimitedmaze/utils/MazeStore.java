package cpe231.finalproject.timelimitedmaze.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class MazeStore {

    private static final MazeParser PARSER = new MazeParser();
    private static final Map<String, Maze> CACHE = new ConcurrentHashMap<>();

    private MazeStore() {
    }

    public static Maze getMaze(String fileName) {
        Objects.requireNonNull(fileName, "fileName cannot be null");
        return CACHE.computeIfAbsent(fileName, MazeStore::loadMaze);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Maze loadMaze(String fileName) {
        List<String> lines = MazeReader.readMaze(fileName);
        return PARSER.parse(fileName, lines);
    }
}
