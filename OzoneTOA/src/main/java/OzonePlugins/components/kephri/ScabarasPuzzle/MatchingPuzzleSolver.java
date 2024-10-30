package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.Utils;
import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.scene.Tiles;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.util.*;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Singleton
public class MatchingPuzzleSolver implements PluginLifecycleComponent {
    private static final Map<Integer, String> TILE_NAMES = ImmutableMap.<Integer, String>builder()
            //flipped
            .put(45365, "Line") // line
            .put(45366, "Knives") // knives
            .put(45367, "Crook") // crook
            .put(45368, "Diamond") // diamond
            .put(45369, "Hand") // hand
            .put(45370, "Star") // star
            .put(45371, "Bird") // bird
            .put(45372, "Wiggle") // wiggle
            .put(45373, "Boot") // boot
            //unflipped
            .put(45356, "Line") // line
            .put(45357, "Knives") // knives
            .put(45358, "Crook") // crook
            .put(45359, "Diamond") // diamond
            .put(45360, "Hand") // hand
            .put(45361, "Star") // star
            .put(45362, "Bird") // bird
            .put(45363, "Wiggle") // wiggle
            .put(45364, "Boot") // boot
            .build();

    private static final Map<Integer, Integer> MATCHED_OBJECT_IDS = ImmutableMap.<Integer, Integer>builder()
            .put(45388, 45365) // line
            .put(45389, 45366) // knives
            .put(45386, 45367) // crook
            .put(45391, 45368) // diamond
            .put(45392, 45369) // hand
            .put(45387, 45370) // star
            .put(45393, 45371) // bird
            .put(45394, 45372) // wiggle
            .put(45395, 45373) // boot
            .build();

    private static final Point[] northPuzzleTiles = {
            new Point(65, 56),
            new Point(67, 56),
            new Point(69, 56),
            new Point(65, 54),
            new Point(67, 54),
            new Point(69, 54),
            new Point(65, 52),
            new Point(67, 52),
            new Point(69, 52),
    };

    private static final Point[] southPuzzleTiles = {
            new Point(65, 40),
            new Point(67, 40),
            new Point(69, 40),
            new Point(65, 42),
            new Point(67, 42),
            new Point(69, 42),
            new Point(65, 44),
            new Point(67, 44),
            new Point(69, 44),
    };

    private final Client client;
    private final EventBus eventBus;
    private final ScabarasManager scabarasManager;
    private final Utils util;

    private int matchedTilesCount;
    private int gameTick;
    private Point refTile = new Point(65,50);

    //states
    private String currentTile;
    private boolean isFlipped;
    private WorldPoint tileLocation;
    private int tileID;

    @Getter
    private final Map<String, MatchingTile> discoveredTiles = Map.of(
            "Line", new MatchingTile(45365,45356),
            "Knives", new MatchingTile(45366,45357),
            "Crook", new MatchingTile(45367,45358),
            "Diamond", new MatchingTile(45368,45359),
            "Hand", new MatchingTile(45369,45360),
            "Star", new MatchingTile(45370,45361),
            "Bird", new MatchingTile(45371,45362),
            "Wiggle", new MatchingTile(45372,45363),
            "Boot", new MatchingTile(45373,45364)
            );
    private final Set<Integer> matchedTiles = new HashSet<>(9);

    @Override
    public boolean isEnabled(RaidState raidState) {
        return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
    }

    @Override
    public void startUp() {
        eventBus.register(this);
        matchedTiles.clear();
        currentTile = "";
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned e) {
        int id = e.getGroundObject().getId();
        if (TILE_NAMES.containsKey(id))
        {
            LocalPoint lp = e.getGroundObject().getLocalLocation();
            LocalPoint middle = LocalPoint.fromScene(refTile.getX(), refTile.getY(),client.getLocalPlayer().getWorldView());
            String name = TILE_NAMES.get(id);
            if((util.getDirection(middle.getX(), middle.getY(), lp.getX(), lp.getY()) & 8) == 0 ) //south
            {
                discoveredTiles.get(name).setLocalPointS(lp);
            }
            else
            {
                discoveredTiles.get(name).setLocalPointN(lp);
            }
        }
        if(id == tileID)
        {
            System.out.println("ongroundobjectspawned checker");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        int gameId = e.getGameObject().getId();
        if (MATCHED_OBJECT_IDS.containsKey(gameId)) {
            MatchingTile match = discoveredTiles.get(TILE_NAMES.get(MATCHED_OBJECT_IDS.get(gameId)));
            if (match == null) {
                log.debug("Failed to find discovered tile for game object id {}!", gameId);
                return;
            }
            match.setMatched(true);
            matchedTiles.add(gameId);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (e.getMessage().startsWith("Your party failed to complete the challenge")) {
            discoveredTiles.clear();
        }
    }

    public void run() {
        if(tileLocation != null)
        {
            if(TileObjects.getFirstAt(tileLocation,tileID) == null)
            {
                return;
            }
            else
            {
                System.out.println(currentTile);
                this.tileLocation = null;
                this.tileID = 0;
                this.isFlipped = !isFlipped;
                if(!isFlipped)
                {
                    currentTile = "";
                }
                System.out.println("onGameTick checker ");
            };
        }
        //state checker for tile to flip. Need tileLocation, TileID,
        if (gameTick > 0)
        {
            System.out.println("gametick" + gameTick);
            gameTick--;
            return;
        }
        if (matchedTiles.size() < 9)
        {
            if(currentTile.isEmpty())
            {
                for(Map.Entry<String, MatchingTile> entry : discoveredTiles.entrySet())
                {
                    if(!entry.getValue().isMatched())
                    {
                        currentTile = entry.getKey();
                    }
                }
            }
            LocalPoint dest;
            WorldPoint middle = WorldPoint.fromScene(client.getLocalPlayer().getWorldView(), refTile.getX(), refTile.getY(), 0);
            int direction = util.getDirection(middle.getX(), middle.getY(), client.getLocalPlayer().getWorldX(), client.getLocalPlayer().getWorldY());
            if (!isFlipped)
            {

                dest = (direction & 8) == 0 ? discoveredTiles.get(currentTile).getLocalPointS() :  discoveredTiles.get(currentTile).getLocalPointN();
            }
            else
            {
                dest = (direction & 8) == 0 ? discoveredTiles.get(currentTile).getLocalPointN() :  discoveredTiles.get(currentTile).getLocalPointS();
            }
            this.tileLocation = WorldPoint.fromLocal(client,dest);
            this.tileID = discoveredTiles.get(currentTile).getFlipId();
            TileObjects.getFirstAt(tileLocation,discoveredTiles.get(currentTile).getUnflippedId()).interact("Activate");
            return;
        }
        else
        {
            System.out.println("GO TO BOSS!!");
        }

    }
}
