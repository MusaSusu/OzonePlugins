package OzonePlugins.components.kephri.ScabarasPuzzle;


import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import OzonePlugins.data.RaidRoom;
import com.google.common.collect.EvictingQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ObjectID;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.entities.TileObjects;

import javax.inject.Inject;
import javax.inject.Singleton;

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

	private final EventBus eventBus;
	private final Client client;

	@Getter
	private final EvictingQueue<LocalPoint> points = EvictingQueue.create(5);

	@Getter
	private int completedTiles = 0;

	private boolean puzzleFinished = false;
	private int lastDisplayTick = 0;

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
				if (lastDisplayTick == (lastDisplayTick = client.getTickCount()))
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
		if (!client.getLocalPlayer().isMoving() && points.isEmpty())
		{
			TileObjects.getNearest(ANCIENT_BUTTON_ID).interact(0);
			return;
		}
	}
}
