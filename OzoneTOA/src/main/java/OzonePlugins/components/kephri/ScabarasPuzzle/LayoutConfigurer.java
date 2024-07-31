package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LayoutConfigurer implements PluginLifecycleComponent {

    private final Client client;
    private final EventBus eventBus;

    enum State
    {
        HIGHLIGHT_UPPER,
        HIGHLIGHT_LOWER,
        UNKNOWN,
        ;
    }
    @Getter
    private State state = State.UNKNOWN;


    private static final int OBELISK_ID = 43876; // imposter as 11698 (inactive), 11699 (active)
    private static final Map<Point, State> QUADRANT_STATES = ImmutableMap.of(
            new Point(36, 57), State.HIGHLIGHT_LOWER, // top left
            new Point(53, 57), State.HIGHLIGHT_UPPER, // top right
            new Point(36, 45), State.HIGHLIGHT_UPPER, // bottom left
            new Point(53, 45), State.HIGHLIGHT_LOWER  // bottom right
    );

    private static final int FLAME_ID = ObjectID.BARRIER_45135;
    private static final Point FLAME_UPPER_HALF_LOC = new Point(28, 54);
    private static final Point FLAME_LOWER_HALF_LOC = new Point(28, 42);

    private static final int ANCIENT_BUTTON_ID = ObjectID.ANCIENT_BUTTON;
    private static final int ANCIENT_TABLET_ID = ObjectID.ANCIENT_TABLET;
    private static Point ANCIENT_BUTTON_LOC, ANCIENT_TABLET_LOC;

    private GameObject flameLower, flameUpper;


    @Override
    public boolean isEnabled(RaidState raidState) {
        if (!raidState.isInRaid())
        {
            return false;
        }
        return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
    }

    @Override
    public void startUp() {
        eventBus.register(this);
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);
    }

    private void reset()
    {
        flameLower = null;
        flameUpper = null;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e)
    {
        checkForFlame(e.getGameObject());
        checkForObelisk(e.getGameObject());
    }

    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        if (e.getMessage().startsWith("Your party failed to complete the challenge"))
        {
            reset();
        }
    }

    private void checkForFlame(GameObject obj)
    {
        if (obj.getId() == FLAME_ID)
        {
            Point scenePoint = obj.getSceneMinLocation(); // size 1 so this works
            if (FLAME_UPPER_HALF_LOC.equals(scenePoint))
            {
                flameUpper = obj;
            }
            else if (FLAME_LOWER_HALF_LOC.equals(scenePoint))
            {
                flameLower = obj;
            }
        }
    }

    private void checkForObelisk(GameObject obj)
    {
        if (state != State.UNKNOWN || obj.getId() != OBELISK_ID)
        {
            return;
        }

        State derivedState = QUADRANT_STATES.get(obj.getSceneMinLocation());
        if (derivedState != null)
        {
            System.out.println("Determined that obelisk puzzle is avoided by" +  state);
            state = derivedState;
        }
    }

    public LocalPoint getFlameLocation() {

        if (state == State.HIGHLIGHT_UPPER) {
            return flameUpper.getLocalLocation();
        }
        if(state == State.HIGHLIGHT_LOWER) {
            return flameLower.getLocalLocation();
        }
        return null;
    }
}
