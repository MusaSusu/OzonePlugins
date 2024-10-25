package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.Utils;
import OzonePlugins.data.RaidState;
import OzonePlugins.data.RaidRoom;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.movement.Movement;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LightPuzzleSolver implements PluginLifecycleComponent
{

	@Inject
	private Utils util;
	@Inject
	private ScabarasManager scabarasManager;

	private static final int GROUND_OBJECT_LIGHT_BACKGROUND = 45344;
	private static final int GAME_OBJECT_LIGHT_ENABLED = 45384;

	private static final Point[] SCENE_COORD_STARTS = {
		new Point(36, 56),
		new Point(36, 44),
		new Point(53, 56),
		new Point(53, 44),
	};

	private static final int[] LIGHTS_PUZZLE_XOR_ARRAY = {
		0B01110101,
		0B10111010,
		0B11001101,
		0B11001110,
		0B01110011,
		0B10110011,
		0B01011101,
		0B10101110,
	};

	private final EventBus eventBus;
	private final Client client;

	private int gameTick;
	private boolean solved;
	private boolean completed;
	private boolean queueInitialized;
	private int tileStates = -1; // bitmask northwest to southeast

	private Point startTile;
	private WorldPoint centerTile;
	private WorldArea puzzleArea;

	private List<WorldPoint> clickPoints = new ArrayList<>(8);

	@Getter
	private Set<WorldPoint> flips = Collections.emptySet();

	private HashSet<WorldPoint> puzzleTiles = new HashSet<>(8);

	@Override
	public boolean isEnabled(RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);

		solved = false;
		completed = false;
		queueInitialized = false;
		puzzleArea = null;
		clickPoints.clear();
		solve();
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (e.getGameObject().getId() == GAME_OBJECT_LIGHT_ENABLED)
		{
			solved = false;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		if (e.getGameObject().getId() == GAME_OBJECT_LIGHT_ENABLED)
		{
			solved = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!solved)
		{
			solve();
		}
		if(!completed)
		{
			if(getFlips().isEmpty())
			{
				completed = true;
			}
		}
	}

	private void solve()
	{
		solved = true;

		Tile[][] sceneTiles = client.getScene().getTiles()[client.getPlane()];
		Point tl = findStartTile(sceneTiles);
		this.startTile = tl;
		if (tl == null)
		{
			log.debug("Failed to locate start of light puzzle");
			return;
		}

		this.centerTile = new WorldPoint(startTile.getX() + 2, startTile.getY() - 2,0);
		this.puzzleArea = new WorldArea(WorldPoint.fromScene(client.getLocalPlayer().getWorldView(),startTile.getX() - 2,startTile.getY() - 5,0),WorldPoint.fromScene(client.getLocalPlayer().getWorldView(),startTile.getX() + 4, startTile.getY(),0));
		this.tileStates = readTileStates(sceneTiles, tl);
		this.flips = findSolution(tl);
	}

	private Point findStartTile(Tile[][] sceneTiles)
	{
		for (Point sceneCoordStart : SCENE_COORD_STARTS)
		{
			Tile startTile = sceneTiles[sceneCoordStart.getX()][sceneCoordStart.getY()];
			GroundObject groundObject = startTile.getGroundObject();
			if (groundObject != null && groundObject.getId() == GROUND_OBJECT_LIGHT_BACKGROUND)
			{
				return sceneCoordStart;
			}
		}

		return null;
	}

	private int readTileStates(Tile[][] sceneTiles, Point topLeft)
	{
		int tileStates = 0;
		HashSet<WorldPoint> tiles = new HashSet<>();
		for (int i = 0; i < 8; i++)
		{
			// middle of puzzle has no light
			// skip middle tile
			int tileIx = i > 3 ? i + 1 : i;
			int x = tileIx % 3;
			int y = tileIx / 3;
			Tile lightTile = sceneTiles[topLeft.getX() + (x * 2)][topLeft.getY() - (y * 2)];
			tiles.add(lightTile.getWorldLocation());

			boolean active = Arrays.stream(lightTile.getGameObjects())
				.filter(Objects::nonNull)
				.mapToInt(GameObject::getId)
				.anyMatch(id -> id == GAME_OBJECT_LIGHT_ENABLED);

			log.debug("Read light ({}, {}) as active={}", x, y, active);
			if (active)
			{
				tileStates |= 1 << i;
			}
		}
		puzzleTiles = tiles;
		return tileStates;
	}

	private Set<WorldPoint> findSolution(Point topLeft)
	{
		int xor = 0;
		for (int i = 0; i < 8; i++)
		{
			// invert the state for xor (consider lights out as a 1)
			int mask = 1 << i;
			if ((tileStates & mask) != mask)
			{
				xor ^= LIGHTS_PUZZLE_XOR_ARRAY[i];
			}
		}

		// convert to scene points
		Set<WorldPoint> points = new HashSet<>();
		for (int i = 7; i >= 0; i--)
		{
			int mask = 1 << i;
			if ((xor & mask) == mask)
			{
				// skip middle tile
				int tileIx = i > 3 ? i + 1 : i;
				int x = tileIx % 3;
				int y = tileIx / 3;
				points.add(WorldPoint.fromScene( client.getLocalPlayer().getWorldView(),topLeft.getX() + (x * 2), topLeft.getY() - (y * 2),0));
			}
		}
		return points;
	}

	public void run()
	{
		if(gameTick > 0)
		{
			gameTick--;
			return;
		}
		if (!completed && solved)
		{
			if (getFlips().isEmpty())
			{
				log.debug("Puzzle not complete but flips is empty");
				return;
			}
			if(!puzzleArea.contains(client.getLocalPlayer().getWorldLocation()))
			{
				if(!client.getLocalPlayer().isMoving())
				{
					Movement.walkTo(puzzleArea);
				}
				return;
			}
			if(!queueInitialized)
			{
				findPathClickPoints();
				queueInitialized = true;
				return;
			}
			WorldPoint dest = clickPoints.get(flips.size()-1);
			List<WorldPoint> path = util.createPath(client.getLocalPlayer().getWorldLocation(), dest, puzzleArea, puzzleTiles,Collections.emptySet());
			if(path.isEmpty())
			{
				System.out.println("empty");
				return;
			}
			Movement.walk(path.get(0));
			int distance = client.getLocalPlayer().getWorldLocation().distanceTo(path.get(0));
			gameTick = (int) Math.ceil((double) distance); //TODO: more accurate gametick
			return;
        }
		if(completed)
		{
			System.out.println("done");
			scabarasManager.walkNextPuzzle();
		}
	}

	private void findPathClickPoints()
	{
		int[][] flipsArray = new int[flips.size() + 1][2];
		flipsArray[0][0] = client.getLocalPlayer().getWorldLocation().getWorldX();
		flipsArray[0][1] = client.getLocalPlayer().getWorldLocation().getWorldY();
		int index = 1;
		for (WorldPoint tile : getFlips())
		{
			flipsArray[index][0] = tile.getWorldX();
			flipsArray[index][1] = tile.getWorldY();
			index++;
		}

		double[][] distMatrix = TSPWithoutReturn.createDistanceMatrix(flipsArray);

		// Solve the TSP without returning to the starting point
		int[] result = TSPWithoutReturn.heldKarp(distMatrix);

		for(int i = result.length - 1 ; i > 0; i--)
		{
			clickPoints.add(new WorldPoint(flipsArray[result[i]][0], flipsArray[result[i]][1],0));
			System.out.println(Arrays.toString(flipsArray[i]));
		}
		System.out.println("x" + puzzleArea.getX() + "y" + puzzleArea.getY());
		System.out.println(clickPoints);
	}

	public static class TSPWithoutReturn {

		// Function to calculate the Manhattan distance between two points
		public static double manhattanDistance(int[] p1, int[] p2) {
			return Math.sqrt(Math.pow((p1[0] - p2[0]),2) + Math.pow( p1[1] - p2[1], 2));
		}

		// Function to create the distance matrix based on Manhattan distance
		public static double[][] createDistanceMatrix(int[][] goals) {
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
		public static int[] heldKarp(double[][] dist) {
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
	}
}
