package OzonePlugins.components.kephri.ScabarasPuzzle;


import OzonePlugins.Utils;
import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.Movement;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdditionPuzzleSolver implements PluginLifecycleComponent
{

	@RequiredArgsConstructor
	@Getter
	enum AdditionTile
	{
		LINE(1, 45345, 45388),
		KNIVES(2, 45346, 45389),
		TRIANGLE(3, 45347, 45390),
		DIAMOND(4, 45348, 45391),
		HAND(5, 45349, 45392),
		BIRD(6, 45350, 45393),
		CROOK(7, 45351, 45386),
		WIGGLE(8, 45352, 45394),
		FOOT(9, 45353, 45395),
		;

		private final int value;
		private final int groundObjectId;
		private final int gameObjectId;
	}

	private static final Set<Integer> GAME_OBJECT_IDS = Arrays.stream(AdditionTile.values())
		.map(AdditionTile::getGameObjectId)
		.collect(Collectors.toSet());

	private static final Point[] SCENE_COORD_STARTS = {
		new Point(36, 56),
		new Point(36, 44),
		new Point(53, 56),
		new Point(53, 44),
	};

	private static final Map<Integer, Set<Integer>> OPTIMAL_SOLUTIONS = ImmutableMap.<Integer, Set<Integer>>builder()
		.put(20, ImmutableSet.of(5, 11, 17))
		.put(21, ImmutableSet.of(10, 11, 17))
		.put(22, ImmutableSet.of(10, 11, 12, 18, 24))
		.put(23, ImmutableSet.of(5, 6, 7, 8, 14))
		.put(24, ImmutableSet.of(5, 11, 17, 23))
		.put(25, ImmutableSet.of(10, 11, 12, 13))
		.put(26, ImmutableSet.of(9, 10, 11, 12, 13))
		.put(27, ImmutableSet.of(5, 6, 7, 8, 4))
		.put(28, ImmutableSet.of(0, 1, 7, 13, 19))
		.put(29, ImmutableSet.of(10, 11, 12, 13, 19))
		.put(30, ImmutableSet.of(10, 11, 12, 13, 14))
		.put(31, ImmutableSet.of(0, 6, 12, 13, 14))
		.put(32, ImmutableSet.of(2, 3, 4, 6, 10))
		.put(33, ImmutableSet.of(4, 5, 6, 7, 8, 9, 14))
		.put(34, ImmutableSet.of(10, 11, 12, 13, 14, 19))
		.put(35, ImmutableSet.of(9, 10, 11, 12, 13, 14, 19))
		.put(36, ImmutableSet.of(0, 1, 2, 3, 4, 9, 14))
		.put(37, ImmutableSet.of(10, 11, 12, 13, 14, 19, 24))
		.put(38, ImmutableSet.of(0, 5, 6, 10, 12, 18, 24))
		.put(39, ImmutableSet.of(2, 3, 4, 7, 10, 11, 12))
		.put(40, ImmutableSet.of(4, 9, 10, 11, 12, 13, 14))
		.put(41, ImmutableSet.of(0, 4, 6, 9, 12, 13, 14))
		.put(42, ImmutableSet.of(0, 5, 9, 10, 11, 12, 13))
		.put(43, ImmutableSet.of(0, 1, 5, 7, 10, 13, 19))
		.put(44, ImmutableSet.of(0, 5, 10, 11, 14, 17, 18))
		.put(45, ImmutableSet.of(0, 1, 2, 3, 4, 5, 10))
		.build();

	private static final Pattern TARGET_NUMBER_PATTERN = Pattern.compile("The number (\\d+) has been hastily chipped into the stone.");

	private final EventBus eventBus;
	private final Client client;
	@Inject
	private Utils util;
	@Inject
	private ScabarasManager scabarasManager;

	private int gameTick;
	private boolean solved;
	private Set<Integer> tileStates;
	private int targetNumber;
	private boolean puzzleComplete;

	private WorldArea puzzleArea;

	@Getter
	private Set<WorldPoint> flips = Collections.emptySet();

	private HashSet<WorldPoint> puzzleTiles = new HashSet<>(25);
	private boolean initialized;

	private List<WorldPoint> clickPoints = new ArrayList<>();
	private int clickPointIndex;
	private boolean pathInitialized;

	private WorldPoint dest = null;

	@Override
	public boolean isEnabled(RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);

		this.targetNumber = 0;
		solved = false;
		initialized = false;
		pathInitialized = false;
		puzzleComplete = false;
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (GAME_OBJECT_IDS.contains(e.getGameObject().getId()))
		{
			solved = false;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		if (GAME_OBJECT_IDS.contains(e.getGameObject().getId()))
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
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getMessage().startsWith("Your party failed to complete the challenge"))
		{
			this.targetNumber = 0;
			solved = false;
			return;
		}

		Matcher matcher = TARGET_NUMBER_PATTERN.matcher(Text.removeTags(e.getMessage()));
		if (!matcher.matches())
		{
			return;
		}

		this.targetNumber = Integer.parseInt(matcher.group(1));
		solve();
	}

	private void solve()
	{
		solved = true;
		if (this.targetNumber < 20)
		{
			return;
		}

		Tile[][] sceneTiles = client.getScene().getTiles()[client.getPlane()];
		Point tl = findStartTile(sceneTiles);
		if (tl == null)
		{
			log.debug("Failed to locate start of addition puzzle");
			return;
		}
		if(puzzleArea == null)
		{
			WorldPoint swTile = WorldPoint.fromScene(client.getLocalPlayer().getWorldView(),tl.getX() - 3, tl.getY() - 5,0 );
			puzzleArea = swTile.createWorldArea(7,7);
		}

		this.tileStates = readTileStates(sceneTiles, tl);
		this.flips = findSolution(tl);
		if(this.flips.size() == 0)
		{
			this.puzzleComplete = true;
		}
	}

	private Point findStartTile(Tile[][] sceneTiles)
	{
		for (Point sceneCoordStart : SCENE_COORD_STARTS)
		{
			Tile startTile = sceneTiles[sceneCoordStart.getX()][sceneCoordStart.getY()];
			GroundObject groundObject = startTile.getGroundObject();
			if (groundObject != null && groundObject.getId() == AdditionTile.FOOT.getGroundObjectId())
			{
				return sceneCoordStart;
			}
		}

		return null;
	}

	private Set<Integer> readTileStates(Tile[][] sceneTiles, Point topLeft)
	{
		Set<Integer> tileStates = new HashSet<>();
		for (int y = 0; y < 5; y++)
		{
			for (int x = 0; x < 5; x++)
			{
				Tile additionTile = sceneTiles[topLeft.getX() + x][topLeft.getY() - y];
				if(!initialized)
				{
					this.puzzleTiles.add(additionTile.getWorldLocation());
				}
				boolean active = Arrays.stream(additionTile.getGameObjects())
					.filter(Objects::nonNull)
					.mapToInt(GameObject::getId)
					.anyMatch(GAME_OBJECT_IDS::contains);
				if (active)
				{
					tileStates.add(y * 5 + x);
				}
			}
		}
		initialized = true;

		return tileStates;
	}

	// todo dynamic solving for when the user messes up
	private Set<WorldPoint> findSolution(Point topLeft)
	{
		Set<Integer> remaining = Sets.difference(OPTIMAL_SOLUTIONS.get(targetNumber), this.tileStates);

		return remaining.stream()
			.map(i -> WorldPoint.fromScene(client,topLeft.getX() + i % 5, topLeft.getY() - i / 5,0))
			.collect(Collectors.toSet());
	}

	public void run()
	{
		if(gameTick > 0)
		{
			gameTick--;
			return;
		}
		if(!puzzleComplete)
		{
			if(this.targetNumber < 20)
			{
				if(!client.getLocalPlayer().isMoving())
				{
					TileObjects.getNearest(ObjectID.ANCIENT_TABLET).interact(0);
					return;
				}
            }
			else
			{
				if (!pathInitialized)
				{
					findPathClickPoints();
					pathInitialized = true;
					return;
				}
				if (this.dest != null)
				{
					if (client.getLocalPlayer().getWorldLocation().equals(dest))
					{
						this.dest = null;
						clickPointIndex++;
					}
				}
				if(clickPointIndex > clickPoints.size() - 1)
				{
					return;
				}

				this.dest = clickPoints.get(clickPointIndex);
				Movement.walk(this.dest);
				int distance = client.getLocalPlayer().getWorldLocation().distanceTo2D(dest);
				System.out.println("gametick" + distance);
				gameTick = (int) Math.ceil((double) distance / 2);
            }
        }
		else
		{
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

		List<WorldPoint> path = util.createPath(client.getLocalPlayer().getWorldLocation(),new WorldPoint(flipsArray[result[1]][0],flipsArray[result[1]][1],0),puzzleArea,puzzleTiles,Collections.emptySet());
		for(int i = 0; i < path.size() - 1; i++)
		{
			clickPoints.add(path.get(i));
		}

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
