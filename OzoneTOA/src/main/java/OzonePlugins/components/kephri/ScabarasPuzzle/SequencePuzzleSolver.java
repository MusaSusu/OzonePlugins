package OzonePlugins.components.kephri.ScabarasPuzzle;


import OzonePlugins.Utils;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import OzonePlugins.data.RaidRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.entities.TileObjects;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SequencePuzzleSolver implements PluginLifecycleComponent
{

	@Inject
	private Utils util;
	private static final int GROUND_OBJECT_ID = 45340;
	private static final int DISPLAY_GAME_OBJECT_ID = 45341;
	private static final int STEPPED_GAME_OBJECT_ID = 45342;
	private static final int GRAPHICS_OBJECT_RESET = 302;
	private static final int ANCIENT_BUTTON_ID =  ObjectID.ANCIENT_BUTTON;
	private Point puzzleRefTile;
	private WorldArea worldArea;

	private final EventBus eventBus;
	private final Client client;

	private final HashSet<WorldPoint> puzzleTiles = new HashSet<>(9);
	private final HashSet<WorldPoint> blockedTiles = new HashSet<>(1);
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

	@Inject
	private ScabarasManager scabarasManager;

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
				WorldPoint swTile = WorldPoint.fromScene(client,e.getGameObject().getSceneMinLocation().getX() - 1,e.getGameObject().getSceneMinLocation().getY() - 4,0);
				Arrays.stream(SCENE_COORD_OFFSETS).forEach(
						i -> puzzleTiles.add(WorldPoint.fromScene(
								client,
								puzzleRefTile.getX() + i.getX(),
								puzzleRefTile.getY() + i.getY(),
								0
						))
				);
				worldArea = swTile.createWorldArea(7,5);
				blockedTiles.add(e.getGameObject().getWorldLocation());
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
		puzzleTiles.clear();
		blockedTiles.clear();
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
			gameTick = util.checkPath(WorldPoint.fromLocal(client,dest),puzzleTiles);
			return;
		}
		if(puzzleFinished)
		{
			scabarasManager.walkNextPuzzle();
		}
	}

}
