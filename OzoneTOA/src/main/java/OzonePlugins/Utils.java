package OzonePlugins;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.unethicalite.api.movement.Movement;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class Utils
{
    @Inject
    private Client client;

    private static @NotNull List<WorldPoint> getWorldPoints() {
        WorldPoint north = new WorldPoint(0,1,0);
        WorldPoint south = new WorldPoint(0,-1,0);
        WorldPoint east = new WorldPoint(1,0,1);
        WorldPoint west = new WorldPoint(-1,0,1);
        WorldPoint northEast = new WorldPoint(1,1,-1);
        WorldPoint northWest = new WorldPoint(-1,1,-1);
        WorldPoint southEast = new WorldPoint(1,-1,1);
        WorldPoint southWest = new WorldPoint(-1,-1,1);
        List<WorldPoint> directions = List.of(north, east, south, west,northEast, northWest, southEast, southWest);
        return directions;
    }
    
    public int checkPath(WorldPoint dest, HashSet<WorldPoint> puzzleTiles)
    {
        List<WorldPoint> path = client.getLocalPlayer().getWorldLocation().pathTo(client,dest);
        WorldPoint start  = client.getLocalPlayer().getWorldLocation();
        WorldPoint safeTile = start;

        if(dest.equals(start))
        {
            return 0;
        }
        int count = 0;
        for (WorldPoint p : path)
        {
            int distance = start.distanceTo(p);
            WorldPoint direction =  new WorldPoint((p.getWorldX() - start.getWorldX()) / distance, (p.getWorldY() - start.getWorldY()) / distance, 0);
            for(int i = 1; i <= distance; i++)
            {
                count++;
                WorldPoint tile = new WorldPoint(start.getWorldX() + (i * direction.getWorldX()), start.getWorldY() + (i * direction.getWorldY() ),0 );
                if( (count & 1) == 0)
                {
                    if (puzzleTiles.contains(tile) && !Objects.equals(tile,dest) )
                    {
                        if(!Objects.equals(safeTile,client.getLocalPlayer().getWorldLocation()))
                        {
                            Movement.walk(safeTile);
                            return (int) Math.ceil((double) count / 2);
                        }
                        else{
                            return findOtherDirection(start,dest,puzzleTiles);
                        }
                    }
                }
                if(!puzzleTiles.contains(tile))
                {
                    safeTile = tile;
                }
            }
            start = p;
        }
        Movement.walk(dest);
        return (int) Math.ceil((double) count /2);
    }

    private int findOtherDirection(WorldPoint start,WorldPoint dest,HashSet<WorldPoint> puzzleTiles)
    {
        List<WorldPoint> directions = getWorldPoints();

        float distance = start.distanceTo2DHypotenuse(dest);
        for (WorldPoint dir : directions)
        {
            WorldPoint step = new WorldPoint(start.getWorldX() + dir.getWorldX(), start.getWorldY() + dir.getWorldY(), 0);
            if(!puzzleTiles.contains(step) && step.distanceTo2DHypotenuse(dest) < distance)
            {
                Movement.walk(step);
                return 1;
            }
        }
        return 0;
    }

    public List<WorldPoint> createPath(WorldPoint from, WorldPoint target, WorldArea worldArea, Set<WorldPoint> blockedTiles)
    {
        //max iteration would be the area of the current tile to the target tile.

        int[][] directions = new int[128][128];
        int[][] distances = new int[128][128];
        int[][] blocked = new int[128][128];
        int[] bufferX = new int[4096];
        int[] bufferY = new int[4096];

        // Initialise directions and distances
        for (int i = 0; i < 128; ++i)
        {
            for (int j = 0; j < 128; ++j)
            {
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        //normalize points
        int currentX = from.getWorldX() - worldArea.getX();
        int currentY = from.getWorldY() - worldArea.getY();
        int minX = 0;
        int maxX = worldArea.getWidth();
        int minY = 0;
        int maxY = worldArea.getHeight();

        //convert set to 2-D array
        for (WorldPoint blockedTile : blockedTiles)
        {
            blocked[blockedTile.getWorldX() - worldArea.getX()][blockedTile.getWorldY() - worldArea.getY()] = 1;
        }

        blocked[target.getWorldX() - worldArea.getX()][target.getWorldY() - worldArea.getY()] = 0;

        int index1 = 0;
        bufferX[0] = currentX;
        int index2 = 1;
        bufferY[0] = currentY;
        distances[currentX][currentY] = 0;

        boolean isReachable = false;

        while (index1 != index2)
        {
            currentX = bufferX[index1];
            currentY = bufferY[index1];
            index1 = index1 + 1 & 4095;
            int currentDistance = distances[currentX][currentY] + 1;
            int WorldAreaX = currentX + worldArea.getX();
            int WorldAreaY = currentY + worldArea.getY();
            if ((WorldAreaX == target.getX()) && (WorldAreaY == target.getY()))
            {
                isReachable = true;
                System.out.println("current distance for first" + currentDistance);
                break;
            }

            if (currentX > minX && directions[currentX - 1][currentY] == 0 && blocked[currentX - 1][currentY] == 0)
            {
                // Able to move 1 tile west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY] = 2;
                distances[currentX - 1][currentY] = currentDistance;
            }

            if (currentX < 127 && directions[currentX + 1][currentY] == 0 && (blocked[currentX + 1][currentY]) == 0)
            {
                // Able to move 1 tile east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY] = 8;
                distances[currentX + 1][currentY] = currentDistance;
            }

            if (currentY > 0 && directions[currentX][currentY - 1] == 0 && (blocked[currentX][currentY - 1]) == 0)
            {
                // Able to move 1 tile south
                bufferX[index2] = currentX;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX][currentY - 1] = 1;
                distances[currentX][currentY - 1] = currentDistance;
            }

            if (currentY < 127 && directions[currentX][currentY + 1] == 0 && (blocked[currentX][currentY + 1]) == 0)
            {
                // Able to move 1 tile north
                bufferX[index2] = currentX;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX][currentY + 1] = 4;
                distances[currentX][currentY + 1] = currentDistance;
            }

            if (currentX > 0 && currentY > 0 && directions[currentX - 1][currentY - 1] == 0 && (blocked[currentX - 1][currentY - 1]) == 0 && (blocked[currentX - 1][currentY]) == 0 && (blocked[currentX][currentY - 1]) == 0)
            {
                // Able to move 1 tile south-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY - 1] = 3;
                distances[currentX - 1][currentY - 1] = currentDistance;
            }

            if (currentX > 0 && currentY < 127 && directions[currentX - 1][currentY + 1] == 0 && (blocked[currentX - 1][currentY + 1]) == 0 && (blocked[currentX - 1][currentY]) == 0 && (blocked[currentX][currentY + 1]) == 0)
            {
                // Able to move 1 tile north-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY + 1] = 6;
                distances[currentX - 1][currentY + 1] = currentDistance;
            }

            if (currentX < 127 && currentY > 0 && directions[currentX + 1][currentY - 1] == 0 && (blocked[currentX + 1][currentY - 1]) == 0 && (blocked[currentX + 1][currentY]) == 0 && (blocked[currentX][currentY - 1]) == 0)
            {
                // Able to move 1 tile south-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY - 1] = 9;
                distances[currentX + 1][currentY - 1] = currentDistance;
            }

            if (currentX < 127 && currentY < 127 && directions[currentX + 1][currentY + 1] == 0 && (blocked[currentX + 1][currentY + 1]) == 0 && (blocked[currentX + 1][currentY]) == 0 && (blocked[currentX][currentY + 1]) == 0)
            {
                // Able to move 1 tile north-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY + 1] = 12;
                distances[currentX + 1][currentY + 1] = currentDistance;
            }
        }
        if(!isReachable)
        {
            System.out.println("not reachable");
        }

        // Getting path from directions and distances
        bufferX[0] = currentX;
        bufferY[0] = currentY;
        int index = 1;
        int directionNew;
        int directionOld;
        for (directionNew = directionOld = directions[currentX][currentY]; from.getX() - worldArea.getX() != currentX || from.getY() - worldArea.getY() != currentY; directionNew = directions[currentX][currentY])
        {
            if (directionNew != directionOld)
            {
                // "Corner" of the path --> new checkpoint tile
                directionOld = directionNew;
                bufferX[index] = currentX;
                bufferY[index++] = currentY;
            }

            if ((directionNew & 2) != 0)
            {
                ++currentX;
            }
            else if ((directionNew & 8) != 0)
            {
                --currentX;
            }

            if ((directionNew & 1) != 0)
            {
                ++currentY;
            }
            else if ((directionNew & 4) != 0)
            {
                --currentY;
            }
        }

        int checkpointTileNumber = 1;
        List<WorldPoint> checkpointTiles = new ArrayList<>();
        while (index-- > 0)
        {
            checkpointTiles.add(new WorldPoint(bufferX[index] + worldArea.getX(),bufferY[index] + worldArea.getY(),from.getPlane()));
            if (checkpointTileNumber == 25)
            {
                // Pathfinding only supports up to the 25 first checkpoint tiles
                break;
            }
            checkpointTileNumber++;
        }
        return checkpointTiles;
    }

    public List<WorldPoint> createPath(WorldPoint from, WorldPoint target, WorldArea worldArea, Set<WorldPoint> blockedTiles,Set<WorldPoint> skipBlockedTiles)
    {
        //max iteration would be the area of the current tile to the target tile.
        if(Objects.equals(from,target))
        {
            System.out.println("already at destination");
            return Collections.emptyList();
        }

        int[][] directions = new int[128][128];
        int[][] distances = new int[128][128];
        int[][] blocked = new int[128][128];
        int[][] skipBlocked = new int[128][128];
        int[] bufferX = new int[4096];
        int[] bufferY = new int[4096];

        // Initialise directions and distances
        for (int i = 0; i < 128; ++i)
        {
            for (int j = 0; j < 128; ++j)
            {
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        //normalize points
        int currentX = from.getWorldX() - worldArea.getX();
        int currentY = from.getWorldY() - worldArea.getY();
        int minX = 0;
        int maxX = worldArea.getWidth();
        int minY = 0;
        int maxY = worldArea.getHeight();

        //convert set to 2-D array
        for (WorldPoint blockedTile : blockedTiles)
        {
            blocked[blockedTile.getWorldX() - worldArea.getX()][blockedTile.getWorldY() - worldArea.getY()] = 1;
        }

        for (WorldPoint skipBlockedTile : skipBlockedTiles)
        {
            skipBlocked[skipBlockedTile.getWorldX() - worldArea.getX()][skipBlockedTile.getWorldY() - worldArea.getY()] = 1;
        }

        //make sure blocked does not contain our dest location
        skipBlocked[target.getWorldX()-worldArea.getX()][target.getWorldY()-worldArea.getY()] = 0;
        blocked[target.getWorldX()-worldArea.getX()][target.getWorldY()-worldArea.getY()] = 0;
        skipBlocked[from.getWorldX() - worldArea.getX()][from.getWorldY() - worldArea.getY()] = 0;

        int index1 = 0;
        bufferX[0] = currentX;
        int index2 = 1;
        bufferY[0] = currentY;
        distances[currentX][currentY] = 0;

        boolean isReachable = false;

        while (index1 != index2)
        {
            currentX = bufferX[index1];
            currentY = bufferY[index1];
            boolean isCurrentSkipBlock = index1 != 0 && skipBlocked[currentX][currentY] == 1;
            index1 = index1 + 1 & 4095;
            int currentDistance = distances[currentX][currentY] + 1;
            int WorldAreaX = currentX + worldArea.getX();
            int WorldAreaY = currentY + worldArea.getY();
            if ((WorldAreaX == target.getX()) && (WorldAreaY == target.getY()))
            {
                isReachable = true;
                break;
            }

            if (currentX > minX && canMoveTo(currentX - 1, currentY, directions, blocked, skipBlocked,isCurrentSkipBlock))
            {
                // Able to move 1 tile west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY] = 2;
                distances[currentX - 1][currentY] = currentDistance;
            }

            if (currentX < 127 && canMoveTo(currentX + 1, currentY, directions, blocked, skipBlocked, isCurrentSkipBlock))
            {
                // Able to move 1 tile east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY] = 8;
                distances[currentX + 1][currentY] = currentDistance;
            }

            if (currentY > 0 && canMoveTo(currentX, currentY - 1, directions, blocked, skipBlocked, isCurrentSkipBlock))
            {
                // Able to move 1 tile south
                bufferX[index2] = currentX;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX][currentY - 1] = 1;
                distances[currentX][currentY - 1] = currentDistance;
            }

            if (currentY < 127 && canMoveTo(currentX, currentY + 1, directions, blocked, skipBlocked, isCurrentSkipBlock))
            {
                // Able to move 1 tile north
                bufferX[index2] = currentX;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX][currentY + 1] = 4;
                distances[currentX][currentY + 1] = currentDistance;
            }

            if (currentX > 0 && currentY > 0 && canMoveTo(currentX - 1, currentY - 1, directions, blocked, skipBlocked, isCurrentSkipBlock) && ((blocked[currentX - 1][currentY]) == 0 || (blocked[currentX][currentY - 1]) == 0))
            {
                // Able to move 1 tile south-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY - 1] = 3;
                distances[currentX - 1][currentY - 1] = currentDistance;
            }

            if (currentX > 0 && currentY < 127 && canMoveTo(currentX - 1, currentY + 1, directions, blocked, skipBlocked, isCurrentSkipBlock) && ((blocked[currentX - 1][currentY]) == 0 || (blocked[currentX][currentY + 1]) == 0))
            {
                // Able to move 1 tile north-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY + 1] = 6;
                distances[currentX - 1][currentY + 1] = currentDistance;
            }

            if (currentX < 127 && currentY > 0 && canMoveTo(currentX + 1, currentY - 1, directions, blocked, skipBlocked, isCurrentSkipBlock) && ((blocked[currentX + 1][currentY]) == 0 || (blocked[currentX][currentY - 1]) == 0))
            {
                // Able to move 1 tile south-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY - 1] = 9;
                distances[currentX + 1][currentY - 1] = currentDistance;
            }

            if (currentX < 127 && currentY < 127 && canMoveTo(currentX + 1, currentY + 1, directions, blocked, skipBlocked, isCurrentSkipBlock) && ((blocked[currentX + 1][currentY]) == 0 || (blocked[currentX][currentY + 1]) == 0))
            {
                // Able to move 1 tile north-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY + 1] = 12;
                distances[currentX + 1][currentY + 1] = currentDistance;
            }
        }
        if(!isReachable)
        {
            System.out.println("not reachable");
            return Collections.emptyList();
        }

        // Getting path from directions and distances
        bufferX[0] = currentX;
        bufferY[0] = currentY;
        int index = 1;
        int directionNew;
        int directionOld;
        int skipCounter = 0;
        boolean skipBlockEncountered = false;
        for (directionNew = directionOld = directions[currentX][currentY]; from.getX() - worldArea.getX() != currentX || from.getY() - worldArea.getY() != currentY; directionNew = directions[currentX][currentY])
        {
            if(directionNew != directionOld)
            {
                if(!isNaturalDirection(directionOld, directionNew))
                {
                    bufferX[index] = currentX;
                    bufferY[index++] = currentY;
                }
                directionOld = directionNew;
            }

            if ((directionNew & 2) != 0)
            {
                ++currentX;
            }
            else if ((directionNew & 8) != 0)
            {
                --currentX;
            }

            if ((directionNew & 1) != 0)
            {
                ++currentY;
            }
            else if ((directionNew & 4) != 0)
            {
                --currentY;
            }
            if(!skipBlockEncountered)
            {
                if(skipBlocked[currentX][currentY] == 1)
                {
                    skipBlockEncountered = true;
                    skipCounter = 0;
                }
            }
            if (skipBlockEncountered)
            {
                if((++skipCounter & 1) == 0)
                {
                    bufferX[index] = currentX;
                    bufferY[index] = currentY;
                }
                else
                {
                    if(skipBlocked[currentX][currentY] == 0)
                    {
                        skipBlockEncountered = false;
                        index++;
                    }
                }
            }
        }

        int checkpointTileNumber = 1;
        List<WorldPoint> checkpointTiles = new ArrayList<>();
        while (index-- > 0)
        {
            checkpointTiles.add(new WorldPoint(bufferX[index] + worldArea.getX(),bufferY[index] + worldArea.getY(),from.getPlane()));
            if (checkpointTileNumber == 25)
            {
                // Pathfinding only supports up to the 25 first checkpoint tiles
                break;
            }
            checkpointTileNumber++;
        }
        System.out.println("path" + checkpointTiles);
        return checkpointTiles;
    }

    private boolean isWithinBounds(int currentX, int currentY, int minX, int minY, int maxX, int maxY)
    {
        return currentX >= minX && currentX <= maxX && currentY >= minY && currentY <= maxY;
    }

    private boolean isNotBlocked(int x, int y, int[][] directions, int[][] blocked)
    {
        return directions[x][y] == 0 && blocked[x][y] == 0;
    }

    private boolean canMoveTo(int x, int y, int[][] directions, int[][] blocked, int[][] skipBlocked,boolean isCurrentSkipBlock) {
        boolean isNotBlocked = directions[x][y] == 0 && blocked[x][y] == 0;
        boolean skipCondition = !isCurrentSkipBlock || skipBlocked[x][y] == 0;

        return isNotBlocked && skipCondition;
    }

    private boolean isNaturalDirection(int directionOld, int directionNew)
    {
        //naturally bfs won't draw perpendicular movements as BFS would find a shorter path there with diagonal movements
        if(countSetBits(directionOld) == 1) //straight
        {
            return false;
        }
        if(countSetBits(directionNew) == 2) //diagonal
        {
            return false;
        }
        return (directionNew & directionOld) > 0;
    }

    private static int countSetBits(int n) {
        int count = 0;
        for(int i = 0; i < 4; i++)
        {
            count += n & 1; // Check if the least significant bit is 1
            n >>>= 1; // Right shift (unsigned) to check the next bit
        }
        return count;
    }

    public int getDirection(int x1, int y1, int x2, int y2)
    {
        double directionX = x2 - x1;
        double directionY = y2 - y1;

        int direction = 0;

        if(directionX > 0)
        {
            direction = 1; //East
        }
        if(directionX < 0)
        {
            direction = 2; //West
        }

        if (directionY > 0)
        {
            direction |= 4; //north
        }
        if(directionY < 0)
        {
            direction |= 8; //south
        }
        return direction;
    }

    public double manhattanDistance(int[] p1, int[] p2) {
        return Math.sqrt(Math.pow((p1[0] - p2[0]),2) + Math.pow( p1[1] - p2[1], 2));
    }

    // Function to create the distance matrix based on Manhattan distance
    public double[][] createDistanceMatrix(int[][] goals) {
        int n = goals.length;
        double[][] dist = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    dist[i][j] = manhattanDistance(goals[i], goals[j]);
                }
            }
        }
        return dist;
    }

    // Held-Karp Algorithm to find the minimum cost path without returning to the start
    public int[] heldKarp(double[][] dist) {
        int n = dist.length;
        double[][] dp = new double[1 << n][n];
        int[][] backtrack = new int[1 << n][n];

        // Initialize the dp array with a large value (representing infinity)
        for (int i = 0; i < (1 << n); i++) {
            for (int j = 0; j < n; j++) {
                dp[i][j] = Integer.MAX_VALUE;
            }
        }

        // Starting point: Starting from city 0
        dp[1][0] = 0;

        // Fill the dp and backtrack tables
        for (int mask = 1; mask < (1 << n); mask++) {
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) == 0) continue;  // If u is not in the current subset

                for (int v = 0; v < n; v++) {
                    if ((mask & (1 << v)) != 0 || u == v) continue;  // If v is already visited or u == v
                    int newMask = mask | (1 << v);
                    double newCost = dp[mask][u] + dist[u][v];

                    if (newCost < dp[newMask][v]) {
                        dp[newMask][v] = newCost;
                        backtrack[newMask][v] = u;  // Store the previous city
                    }
                }
            }
        }

        // Find the minimum cost to visit all cities (no need to return to the starting point)
        double minCost = Integer.MAX_VALUE;
        int lastCity = -1;
        int finalMask = (1 << n) - 1;

        for (int u = 1; u < n; u++) {
            if (dp[finalMask][u] < minCost) {
                minCost = dp[finalMask][u];
                lastCity = u;
            }
        }

        // Reconstruct the path by backtracking
        int[] path = new int[n];
        int mask = finalMask;
        int currentCity = lastCity;
        for (int i = n - 1; i >= 0; i--) {
            path[i] = currentCity;
            currentCity = backtrack[mask][currentCity];
            mask ^= (1 << path[i]);  // Remove the current city from the visited set
        }

        return path;
    }

    /*

    public void createPath2(WorldPoint current, WorldPoint target, WorldArea worldArea, Set<WorldPoint> blockedTiles) {
        //max iteration would be the area of the current tile to the target tile.

        int[][] directions = new int[128][128];
        int[][] distances = new int[128][128];
        int[] bufferX = new int[4096];
        int[] bufferY = new int[4096];

        // Initialise directions and distances
        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 128; ++j) {
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        //normalize points
        int currentX = current.getWorldX() - worldArea.getX();
        int currentY = current.getWorldY() - worldArea.getY();
        int minX = 0;
        int maxX = worldArea.getWidth();
        int minY = 0;
        int maxY = worldArea.getHeight();


        int index1 = 0;
        bufferX[0] = currentX;
        int index2 = 1;
        bufferY[0] = currentY;
        distances[currentX][currentY] = 0;

        boolean isReachable = false;

        while (index1 != index2) {
            currentX = bufferX[index1];
            currentY = bufferY[index1];
            index1 = index1 + 1 & 4095;
            int currentDistance = distances[currentX][currentY] + 1;
            int WorldAreaX = currentX + worldArea.getX();
            int WorldAreaY = currentY + worldArea.getY();

            if ((WorldAreaX == target.getX()) && (WorldAreaY == target.getY())) {
                isReachable = true;
                System.out.println("current distance" + currentDistance);
                break;
            }
            if (currentX > minX && directions[currentX - 1][currentY] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX - 1,WorldAreaY,0)))
            {
                // Able to move 1 tile west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY] = 2;
                distances[currentX - 1][currentY] = currentDistance;
            }

            if (currentX < maxX && directions[currentX + 1][currentY] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX + 1,WorldAreaY,0))) {
                // Able to move 1 tile east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY] = 8;
                distances[currentX + 1][currentY] = currentDistance;
            }

            if (currentY > 0 && directions[currentX][currentY - 1] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX - 1,WorldAreaY,0))) {
                // Able to move 1 tile south
                bufferX[index2] = currentX;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX][currentY - 1] = 1;
                distances[currentX][currentY - 1] = currentDistance;
            }

            if (currentY < maxY && directions[currentX][currentY + 1] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX,WorldAreaY + 1,0))) {
                // Able to move 1 tile north
                bufferX[index2] = currentX;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX][currentY + 1] = 4;
                distances[currentX][currentY + 1] = currentDistance;
            }

            if (currentX > 0 && currentY > 0 && directions[currentX - 1][currentY - 1] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX - 1,WorldAreaY - 1,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX - 1,WorldAreaY,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX,WorldAreaY - 1,0))) {
                // Able to move 1 tile south-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY - 1] = 3;
                distances[currentX - 1][currentY - 1] = currentDistance;
            }

            if (currentX > 0 && currentY < maxY && directions[currentX - 1][currentY + 1] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX - 1,WorldAreaY + 1 ,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaY - 1,WorldAreaY,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX,WorldAreaY + 1,0))) {
                // Able to move 1 tile north-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX - 1][currentY + 1] = 6;
                distances[currentX - 1][currentY + 1] = currentDistance;
            }

            if (currentX < 127 && currentY > 0 && directions[currentX + 1][currentY - 1] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX + 1,WorldAreaY - 1,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX + 1,WorldAreaY,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX,WorldAreaY - 1,0))) {
                // Able to move 1 tile south-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY - 1] = 9;
                distances[currentX + 1][currentY - 1] = currentDistance;
            }

            if (currentX < 127 && currentY < 127 && directions[currentX + 1][currentY + 1] == 0 && !blockedTiles.contains(new WorldPoint(WorldAreaX + 1,WorldAreaY + 1,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX + 1,WorldAreaY,0)) && !blockedTiles.contains(new WorldPoint(WorldAreaX,WorldAreaY + 1,0))) {
                // Able to move 1 tile north-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentX + 1][currentY + 1] = 12;
                distances[currentX + 1][currentY + 1] = currentDistance;
            }

        }
        if(!isReachable)
        {
            System.out.println("not reachable");
        }
    }
     */

}
