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
	private boolean initialized;
	private boolean inPuzzleArea;
	private int tileStates = -1; // bitmask northwest to southeast

	private Point startTile;
	private WorldPoint centerTile;
	private WorldArea puzzleArea;


	@Getter
	private Set<WorldPoint> flips = Collections.emptySet();

	private final HashSet<WorldPoint> puzzleTiles = new HashSet<>(8);

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
		initialized = false;
		inPuzzleArea = false;
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
		if(puzzleArea == null)
		{
			this.puzzleArea = new WorldArea(new WorldPoint(startTile.getX(),startTile.getY() - 4,0), new WorldPoint(startTile.getX() + 4, startTile.getY(),0));
		}
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
		for (int i = 0; i < 8; i++)
		{
			// middle of puzzle has no light
			// skip middle tile
			int tileIx = i > 3 ? i + 1 : i;
			int x = tileIx % 3;
			int y = tileIx / 3;
			Tile lightTile = sceneTiles[topLeft.getX() + (x * 2)][topLeft.getY() - (y * 2)];
			if(!initialized)
			{
				puzzleTiles.add(lightTile.getWorldLocation());
			}

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

		initialized = true;
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
			if (getFlips().stream().findFirst().isEmpty())
			{
				System.out.println("puzzle not complete but flips is empty");
				return;
			}
			if(!inPuzzleArea)
			{
				if(puzzleArea.contains(client.getLocalPlayer().getWorldLocation()))
				{
					inPuzzleArea = true;
					return;
				}
				WorldPoint firstTile = getFlips().stream().findFirst().get();
				moveFirstTile(firstTile);
			}
			else
			{
				System.out.println("check path running");
				checkPath(getFlips().stream().findFirst().get());
            }
            return;
        }
		else
		{
			scabarasManager.walkNextPuzzle();
		}
	}

	private void checkPath(WorldPoint dest)
	{
		List<WorldPoint> path = client.getLocalPlayer().getWorldLocation().pathTo(client,dest);

		WorldPoint start  = client.getLocalPlayer().getWorldLocation();
		WorldPoint safeTile = start;

		if(dest.equals(start))
		{
			return;
		}

		if (path.size() == 1)
		{
			//if we are on a straight path check if there are more tiles on this path.
			int distance = start.distanceTo(path.get(0));
			WorldPoint direction =  new WorldPoint((path.get(0).getWorldX() - start.getWorldX()) / distance, (path.get(0).getWorldY() - start.getWorldY()) / distance, 0);
			WorldPoint tile = new WorldPoint(dest.getWorldX() + (direction.getWorldX() * 2), dest.getWorldY() + (direction.getWorldY() * 2),0);
			if(flips.contains(tile))
			{
				checkPath(tile);
				return;
			}
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
				if (puzzleTiles.contains(tile) && !flips.contains(tile) )
				{
					Movement.walk(centerTile);
					gameTick = 2 ;
					return;
				}
			}
			start = p;
		}
		Movement.walk(dest);
		gameTick = (int) Math.ceil((double) count /2);
	}

	private void findOtherDirection(WorldPoint start,WorldPoint dest)
	{
		WorldPoint north = new WorldPoint(0,1,0);
		WorldPoint south = new WorldPoint(0,-1,0);
		WorldPoint east = new WorldPoint(1,0,1);
		WorldPoint west = new WorldPoint(-1,0,1);
		WorldPoint northEast = new WorldPoint(1,1,-1);
		WorldPoint northWest = new WorldPoint(-1,1,-1);
		WorldPoint southEast = new WorldPoint(1,-1,1);
		WorldPoint southWest = new WorldPoint(-1,-1,1);
		List<WorldPoint> directions = List.of(north, east, south, west,northEast, northWest, southEast, southWest);

		float distance = start.distanceTo2DHypotenuse(dest);
		for (WorldPoint dir : directions)
		{
			WorldPoint step = new WorldPoint(start.getWorldX() + dir.getWorldX(), start.getWorldY() + dir.getWorldY(), 0);
			if(!puzzleTiles.contains(step) && step.distanceTo2DHypotenuse(dest) < distance)
			{
				Movement.walk(step);
				gameTick = 1;
				return;
			}
		}
	}

	private void moveFirstTile(WorldPoint dest)
	{
		List<WorldPoint> path = client.getLocalPlayer().getWorldLocation().pathTo(client,dest);

		WorldPoint start  = client.getLocalPlayer().getWorldLocation();
		WorldPoint safeTile = start;

		if(dest.equals(start))
		{
			return;
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
				if (puzzleTiles.contains(tile) && !Objects.equals(tile,dest) )
				{
					if (!Objects.equals(safeTile, client.getLocalPlayer().getWorldLocation()))
					{
						Movement.walk(safeTile);
						gameTick = (int) Math.ceil((double) count / 2);
                    }
					else
					{
						findOtherDirection(start, dest);
                    }
                    return;
                }
				if(!puzzleTiles.contains(tile))
				{
					safeTile = tile;
				}
			}
			start = p;
		}
		Movement.walk(dest);
		gameTick = (int) Math.ceil((double) count /2);
	}
}
