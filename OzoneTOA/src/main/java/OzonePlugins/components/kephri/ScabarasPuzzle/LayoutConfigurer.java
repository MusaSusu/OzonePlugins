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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    enum QuadrantState
    {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
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

    private static final Map<Point, QuadrantState> OBELISK_STATES = ImmutableMap.of(
            new Point(36, 57), QuadrantState.TOP_LEFT, // top left
            new Point(53, 57), QuadrantState.TOP_RIGHT, // top right
            new Point(36, 45), QuadrantState.BOTTOM_LEFT, // bottom left
            new Point(53, 45), QuadrantState.BOTTOM_RIGHT  // bottom right
    );

    private static final int FLAME_ID = ObjectID.BARRIER_45135;
    private static final Point FLAME_UPPER_HALF_LOC = new Point(28, 54);
    private static final Point FLAME_LOWER_HALF_LOC = new Point(28, 42);

    private static final int ANCIENT_BUTTON_ID = ObjectID.ANCIENT_BUTTON;
    private static final int ANCIENT_TABLET_ID = ObjectID.ANCIENT_TABLET;

    private static final Map<Point, QuadrantState> BUTTON_STATES = ImmutableMap.of(
            new Point(34,44), QuadrantState.BOTTOM_LEFT,
            new Point(34,56), QuadrantState.TOP_LEFT,
            new Point(51,44), QuadrantState.BOTTOM_RIGHT,
            new Point(51,56), QuadrantState.TOP_RIGHT
    );

    private static Map<QuadrantState,ScabarasState> layout;

    static{
        layout = new HashMap<>();
        layout.put(QuadrantState.BOTTOM_LEFT,null);
        layout.put(QuadrantState.BOTTOM_RIGHT,null);
        layout.put(QuadrantState.TOP_LEFT,null);
        layout.put(QuadrantState.TOP_RIGHT,null);
    }

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
        state = State.UNKNOWN;
        layout.put(QuadrantState.BOTTOM_LEFT,null);
        layout.put(QuadrantState.BOTTOM_RIGHT,null);
        layout.put(QuadrantState.TOP_LEFT,null);
        layout.put(QuadrantState.TOP_RIGHT,null);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e)
    {
        checkForFlame(e.getGameObject());
        checkForObelisk(e.getGameObject());
        checkForAncientButton(e.getGameObject());
        checkForAncientTablet(e.getGameObject());
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

        QuadrantState derivedState = OBELISK_STATES.get(obj.getSceneMinLocation());
        if (derivedState != null)
        {
            System.out.println("Determined that obelisk puzzle is avoided by" +  state);
            if(derivedState == QuadrantState.BOTTOM_LEFT | derivedState == QuadrantState.TOP_RIGHT )
            {
                state = State.HIGHLIGHT_UPPER;
            }
            else
            {
                state = State.HIGHLIGHT_LOWER;
            }
            layout.put(derivedState,ScabarasState.OBELISK_PUZZLE);
        }
    }

    private void checkForAncientButton(GameObject obj)
    {
        if(obj.getId() != ANCIENT_BUTTON_ID)
        {
            return;
        }
        QuadrantState derivedState = BUTTON_STATES.get(obj.getSceneMinLocation());
        if(derivedState != null)
        {
            layout.put(derivedState,ScabarasState.SEQUENCE_PUZZLE);
        }
    }

    private void checkForAncientTablet(GameObject obj)
    {
        if(obj.getId() != ANCIENT_TABLET_ID)
        {
            return;
        }
        QuadrantState derivedState = BUTTON_STATES.get(obj.getSceneMinLocation());
        if(derivedState != null)
        {
            layout.put(derivedState,ScabarasState.ADDITION_PUZZLE);
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

    public ScabarasState getCurrentPuzzle(boolean isfirstPuzzle)
    {
        ScabarasState room;
        if(isfirstPuzzle)
        {
            if (state == State.HIGHLIGHT_UPPER)
            {
                room = layout.get(QuadrantState.TOP_LEFT);
            }
            else
            {
                room = layout.get(QuadrantState.BOTTOM_LEFT);
            }
        }
        else
        {
            if (state == State.HIGHLIGHT_UPPER)
            {
                room = layout.get(QuadrantState.BOTTOM_RIGHT);
            }
            else
            {
                room = layout.get(QuadrantState.TOP_RIGHT);
            }
        }
        return Objects.requireNonNullElse(room, ScabarasState.LIGHT_PUZZLE);
    }
}
