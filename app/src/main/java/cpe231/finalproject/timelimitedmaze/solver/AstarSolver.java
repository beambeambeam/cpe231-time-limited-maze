package cpe231.finalproject.timelimitedmaze.solver;

import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;
import cpe231.finalproject.timelimitedmaze.utils.MazeCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class AstarSolver extends MazeSolver {

    @Override
    public String getAlgorithmName() {
        return "A* Search";
    }

    @Override
    protected List<Coordinate> executeSolve(Maze maze) {
        int rows = maze.getHeight();
        int cols = maze.getWidth();
        Coordinate start = maze.getStart();
        Coordinate goal = maze.getGoal();

        int startRow = start.row();
        int startCol = start.column();
        int goalRow = goal.row();
        int goalCol = goal.column();

        // Use 1D arrays for better cache locality and performance
        int size = rows * cols;
        double[] gScore = new double[size];
        double[] fScore = new double[size];
        int[] parent = new int[size];
        boolean[] closed = new boolean[size];

        // Initialize arrays
        Arrays.fill(gScore, Double.POSITIVE_INFINITY);
        Arrays.fill(fScore, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        int startIndex = startRow * cols + startCol;
        gScore[startIndex] = 0.0;
        fScore[startIndex] = calculateHValue(startRow, startCol, goalRow, goalCol);

        PriorityQueue<Node> openList = new PriorityQueue<>();
        openList.add(new Node(fScore[startIndex], startRow, startCol));

        // Cache grid for direct access to avoid Coordinate object creation
        List<List<MazeCell>> grid = maze.getGrid();

        // Pre-defined directions and costs
        // 0-3: Cardinal (N, S, E, W)
        int[] dRow = { -1, 1, 0, 0 };
        int[] dCol = { 0, 0, 1, -1 };
        double[] dCost = { 1.0, 1.0, 1.0, 1.0 };

        while (!openList.isEmpty()) {
            Node current = openList.poll();
            int r = current.r;
            int c = current.c;
            int currentIndex = r * cols + c;

            // Lazy removal: if we've already closed this node, skip it
            if (closed[currentIndex])
                continue;
            closed[currentIndex] = true;

            // Check if reached goal
            if (r == goalRow && c == goalCol) {
                return reconstructPath(parent, goalRow, goalCol, cols);
            }

            // Check all 4 neighbors
            for (int k = 0; k < 4; k++) {
                int nr = r + dRow[k];
                int nc = c + dCol[k];

                // Bounds check
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    int neighborIndex = nr * cols + nc;

                    if (closed[neighborIndex])
                        continue;

                    MazeCell cell = grid.get(nr).get(nc);
                    if (!cell.isWalkable())
                        continue;

                    double moveCost = dCost[k];
                    double stepCost = cell.stepCost();
                    double tentativeG = gScore[currentIndex] + (moveCost * stepCost);

                    if (tentativeG < gScore[neighborIndex]) {
                        parent[neighborIndex] = currentIndex;
                        gScore[neighborIndex] = tentativeG;
                        double h = calculateHValue(nr, nc, goalRow, goalCol);
                        fScore[neighborIndex] = tentativeG + h;
                        openList.add(new Node(fScore[neighborIndex], nr, nc));
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    private double calculateHValue(int r, int c, int gr, int gc) {
        return Math.abs(r - gr) + Math.abs(c - gc);
    }

    private List<Coordinate> reconstructPath(int[] parent, int gr, int gc, int cols) {
        List<Coordinate> path = new ArrayList<>();
        int currIndex = gr * cols + gc;

        while (currIndex != -1) {
            int r = currIndex / cols;
            int c = currIndex % cols;
            path.add(new Coordinate(r, c));
            currIndex = parent[currIndex];
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node implements Comparable<Node> {
        double f;
        int r, c;

        Node(double f, int r, int c) {
            this.f = f;
            this.r = r;
            this.c = c;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }
}
