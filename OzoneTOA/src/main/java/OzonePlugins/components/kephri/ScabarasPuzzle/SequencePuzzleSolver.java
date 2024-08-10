package OzonePlugins.components.kephri.ScabarasPuzzle;


import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import OzonePlugins.data.RaidRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.Movement;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SequencePuzzleSolver implements PluginLifecycleComponent
{

	private static final int GROUND_OBJECT_ID = 45340;
	private static final int DISPLAY_GAME_OBJECT_ID = 45341;
	private static final int STEPPED_GAME_OBJECT_ID = 45342;
	private static final int GRAPHICS_OBJECT_RESET = 302;
	private static final int ANCIENT_BUTTON_ID =  ObjectID.ANCIENT_BUTTON;
	private Point puzzleRefTile;

	private final EventBus eventBus;
	private final Client client;

	private final HashSet<WorldPoint> puzzleTiles = new HashSet<>(9);
	private final static Point[] SCENE_COORD_OFFSETS = {
            new Point(0, 0),
            new Point(1, 1),
            new Point(1, -1),
            new Point(2, 0),
            new Point(2, 2),
            new Point(2, -2),
            new Point(3, 1),
            new Point(3, -1),
            new Point(4, 0)
    };

	@Getter
	private final List<LocalPoint> points = new ArrayList<>(5);

	@Getter
	private int completedTiles = 0;

	private boolean puzzleFinished = false;
	private int lastDisplayTick = 0;
	private int gameTick = 0;

	@Override
	public boolean isEnabled(RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		reset();
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (puzzleFinished)
		{
			return;
		}

		switch (e.getGameObject().getId())
		{
			case DISPLAY_GAME_OBJECT_ID:
				if (lastDisplayTick == (lastDisplayTick = client.getTickCount()) )
				{
					reset();
					puzzleFinished = true;
					return;
				}
				points.add(e.getTile().getLocalLocation());
				completedTiles = 0;
				break;

			case STEPPED_GAME_OBJECT_ID:
				completedTiles++;
				break;
			case ANCIENT_BUTTON_ID: {
				puzzleRefTile = e.getGameObject().getSceneMinLocation().offset(2,-2);
				Arrays.stream(SCENE_COORD_OFFSETS).forEach(
						i -> puzzleTiles.add(WorldPoint.fromScene(
								client,
								puzzleRefTile.getX() + i.getX(),
								puzzleRefTile.getY() + i.getY(),
								0
						))
				);
				System.out.println(puzzleTiles);
			}
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated e)
	{
		if (e.getGraphicsObject().getId() == GRAPHICS_OBJECT_RESET)
		{
			LocalPoint gLoc = e.getGraphicsObject().getLocation();
			Tile gTile = client.getScene().getTiles()[client.getPlane()][gLoc.getSceneX()][gLoc.getSceneY()];
			if (gTile.getGroundObject() != null && gTile.getGroundObject().getId() == GROUND_OBJECT_ID)
			{
				reset();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getMessage().startsWith("Your party failed to complete the challenge"))
		{
			reset();
		}
	}

	private void reset()
	{
		puzzleFinished = false;
		points.clear();
		completedTiles = 0;
		lastDisplayTick = 0;
	}

	public void run()
	{
		if(gameTick > 0)
		{
			gameTick--;
			return;
		}
		if (!client.getLocalPlayer().isMoving() && points.isEmpty() && !puzzleFinished)
		{
			TileObjects.getNearest(ANCIENT_BUTTON_ID).interact(0);
			return;
		}
		if (completedTiles < 5 && points.size() == 5)
		{
			LocalPoint dest = getPoints().get(completedTiles);
			checkPath(WorldPoint.fromLocal(client,dest));
		}
	}

	private void checkPath(WorldPoint dest)
	{
		List<WorldPoint> path = client.getLocalPlayer().getWorldLocation().pathTo(client,dest);
		WorldPoint start  = client.getLocalPlayer().getWorldLocation();
		WorldPoint safeTile = start;
		List<WorldPoint> fullPath = new ArrayList<>();
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
				if( (count & 1) == 0)
				{
					if (puzzleTiles.contains(tile) && !Objects.equals(tile,dest) )
					{
						if(!Objects.equals(safeTile,client.getLocalPlayer().getWorldLocation()))
						{
							Movement.walk(safeTile);
							gameTick = (int) Math.ceil((double) count / 2);
							return;
						}
						else{
							findOtherDirection(start,dest);
							return;
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
}
