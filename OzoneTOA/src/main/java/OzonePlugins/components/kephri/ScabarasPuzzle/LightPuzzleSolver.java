package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.Utils;
import OzonePlugins.data.RaidState;
import OzonePlugins.data.RaidRoom;
import OzonePlugins.modules.PluginLifecycleComponent;
import com.google.common.collect.Sets;
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
	private int clickPointIndex;
	private WorldPoint dest;

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
			if(this.dest != null)
			{
				if(client.getLocalPlayer().getWorldLocation().equals(dest))
				{
					this.dest = null;
					clickPointIndex++;
				}
			}
			this.dest = clickPoints.get(clickPointIndex);
			Sets.SetView<WorldPoint> blockedTiles = Sets.difference(puzzleTiles,getFlips());
			List<WorldPoint> path = util.createPath(client.getLocalPlayer().getWorldLocation(), dest, puzzleArea, blockedTiles,Collections.emptySet());
			if(path.isEmpty())
			{
				System.out.println("empty");
				return;
			}
			Movement.walk(path.get(0));
			int distance = client.getLocalPlayer().getWorldLocation().distanceTo2D(path.get(0));
			gameTick = (int) Math.ceil((double) distance / 2); //TODO: more accurate gametick
			return;
        }
		if(completed)
		{
			System.out.println("done");
			scabarasManager.walkNextPuzzle();
		}
	}

	private void findPathClickPoints() {
		int[][] flipsArray = new int[flips.size() + 1][2];
		flipsArray[0][0] = client.getLocalPlayer().getWorldLocation().getWorldX();
		flipsArray[0][1] = client.getLocalPlayer().getWorldLocation().getWorldY();
		int index = 1;
		for (WorldPoint tile : getFlips()) {
			flipsArray[index][0] = tile.getWorldX();
			flipsArray[index][1] = tile.getWorldY();
			index++;
		}

		double[][] distMatrix = util.createDistanceMatrix(flipsArray);

		// Solve the TSP without returning to the starting point
		int[] result = util.heldKarp(distMatrix);

		int prevDirection = 0;

		index = 0;
		for (int i = 1; i < result.length - 1; i++)
		{
			//check direction
			int currentDirection = util.getDirection(flipsArray[result[i]][0], flipsArray[result[i]][1], flipsArray[result[i + 1]][0], flipsArray[result[i + 1]][1]);
			if (prevDirection != currentDirection)
			{
				clickPoints.add(new WorldPoint(flipsArray[result[i]][0],flipsArray[result[i]][1],0));
				index++;
			}
			prevDirection = currentDirection;
		}
		clickPoints.add(index,new WorldPoint(flipsArray[result[result.length - 1]][0],flipsArray[result[result.length - 1]][1],0));
		System.out.println(clickPoints);
		this.clickPointIndex = 0;
	}
}
