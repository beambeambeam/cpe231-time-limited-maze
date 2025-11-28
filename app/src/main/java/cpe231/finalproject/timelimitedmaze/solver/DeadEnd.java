package cpe231.finalproject.timelimitedmaze.solver;

import java.util.*;
import cpe231.finalproject.timelimitedmaze.utils.Coordinate;
import cpe231.finalproject.timelimitedmaze.utils.Maze;

public class DeadEnd extends MazeSolver {
    
    private Set<Coordinate> filledCells;
    
    @Override
    protected List<Coordinate> executeSolve(Maze maze) {
        filledCells = new HashSet<>();
        
        // Fill dead-ends
        System.out.println("\n=== Dead-End Filling Phase ===");
        fillDeadEnds(maze);
        
        // Find path using Dijkstra's algorithm
        System.out.println("\n=== Dijkstra's Algorithm Phase ===");
        List<Coordinate> path = findShortestPath(maze);
        
        if (path == null || path.isEmpty()) {
            throw new MazeSolvingException("Dead-End Filling failed to find a path");
        }
        
        return path;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Dead-End Filling + Dijkstra";
    }
    
    private void fillDeadEnds(Maze maze) {
        boolean changed = true;
        int iteration = 0;
        int totalFilled = 0;
        
        while (changed) {
            changed = false;
            iteration++;
            int filledCount = 0;
            
            for (int row = 0; row < maze.getHeight(); row++) {
                for (int col = 0; col < maze.getWidth(); col++) {
                    Coordinate current = new Coordinate(row, col);
                    
                    if (filledCells.contains(current)) continue;
                    if (!isWalkable(maze, current)) continue;
                    if (current.equals(maze.getStart())) continue;
                    if (current.equals(maze.getGoal())) continue;
                    
                    int walkableNeighbors = countWalkableNeighbors(maze, current);
                    
                    if (walkableNeighbors == 1) {
                        filledCells.add(current);
                        changed = true;
                        filledCount++;
                    }
                }
            }
            
            if (filledCount > 0) {
                totalFilled += filledCount;
                System.out.println("Iteration " + iteration + ": Filled " + filledCount + " dead-ends");
            }
        }
        
        System.out.println("Dead-end filling completed: " + totalFilled + 
                         " cells filled in " + (iteration - 1) + " iterations");
    }
    
    private int countWalkableNeighbors(Maze maze, Coordinate coordinate) {
        int count = 0;
        
        for (Direction direction : Direction.values()) {
            Coordinate neighbor = move(coordinate, direction);
            
            if (isWalkable(maze, neighbor) && !filledCells.contains(neighbor)) {
                count++;
            }
        }
        
        return count;
    }
    
    private List<Coordinate> findShortestPath(Maze maze) {
        Coordinate start = maze.getStart();
        Coordinate goal = maze.getGoal();
        
        Map<Coordinate, Integer> distance = new HashMap<>();
        Map<Coordinate, Coordinate> previous = new HashMap<>();
        Set<Coordinate> visited = new HashSet<>();
        
        PriorityQueue<Node> queue = new PriorityQueue<>(
            Comparator.comparingInt(n -> n.distance)
        );
        
        distance.put(start, 0);
        queue.offer(new Node(start, 0));
        
        int explored = 0;
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Coordinate currentCoord = current.coordinate;
            
            if (visited.contains(currentCoord)) {
                continue;
            }
            visited.add(currentCoord);
            explored++;
            
            if (currentCoord.equals(goal)) {
                System.out.println("Path found! Explored " + explored + " cells");
                return reconstructPath(previous, start, goal);
            }
            
            for (Direction direction : Direction.values()) {
                Coordinate neighbor = move(currentCoord, direction);
                
                if (!isWalkable(maze, neighbor)) continue;
                if (filledCells.contains(neighbor)) continue;
                if (visited.contains(neighbor)) continue;
                
                int currentDistance = distance.getOrDefault(currentCoord, Integer.MAX_VALUE);
                
                int edgeWeight = stepCost(maze, neighbor);
                int newDistance = currentDistance + edgeWeight;
                
                int neighborDistance = distance.getOrDefault(neighbor, Integer.MAX_VALUE);
                if (newDistance < neighborDistance) {
                    distance.put(neighbor, newDistance);
                    previous.put(neighbor, currentCoord);
                    queue.offer(new Node(neighbor, newDistance));
                }
            }
        }
        
        System.out.println("Warning: No path found from start to goal after exploring " + explored + " cells");
        return null;
    }
    
    private List<Coordinate> reconstructPath(
            Map<Coordinate, 
            Coordinate> previous, 
            Coordinate start, 
            Coordinate goal
        ) {
        List<Coordinate> path = new ArrayList<>();
        Coordinate current = goal;
        
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        
        Collections.reverse(path);
        
        if (!path.isEmpty() && !path.get(0).equals(start)) {
            System.out.println("Warning: Reconstructed path does not start at start position");
            return null;
        }
        
        System.out.println("Path length: " + path.size() + " cells");
        return path;
    }
    
    private static class Node {
        Coordinate coordinate;
        int distance;
        
        Node(Coordinate coordinate, int distance) {
            this.coordinate = coordinate;
            this.distance = distance;
        }
    }
}