package ozone;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Extension
@PluginDescriptor(
        name = " Ozone Tempoross Assistant",
        description = "Helps with Tempoross",
        tags = {"Minigame","Ozone"}
)
@Slf4j
public class OzoneTempoross extends Plugin {
    @Inject
    private Client client;
    private int waves = 0;
    private int ticks = 0;
    private boolean incomingWave = false;
    private boolean inGame = false;
    private static int ENERGY = 100;
    private static int ESSENCE = 100;
    private static int INTENSITY = 0;
    private State scriptState = State.FISHING;

    private TemporossWorkArea workArea = null;

    private List<WorldPoint> target = null;

    @Subscribe
    public void onGameTick(GameTick event) {
        if (ticks > 0) {
            ticks--;
            return;
        }

        Widget energyWidget = Widgets.get(437, 35);
        Widget essenceWidget = Widgets.get(437, 45);
        Widget intensityWidget = Widgets.get(437, 55);

        if (!Widgets.isVisible(energyWidget) || !Widgets.isVisible(essenceWidget) || !Widgets.isVisible(intensityWidget)) {
            inGame = false;
            TileObject startLadder = TileObjects.getFirstAt(3135, 2840, 0, 41305);
            if (startLadder == null) {
                return;
            }

            // If east of ladder, we're not in the room.
            if (Players.getLocal().getWorldLocation().getX() > startLadder.getWorldLocation().getX()) {
                startLadder.interact("Quick-climb");
                ticks = 8;
            }
        } else {
            inGame = true;
        }

        if (inGame) {
            if (workArea == null) {
                NPC npc = NPCs.getNearest(x -> x.hasAction("Forfeit"));
                NPC ammoCrate = NPCs.getNearest(x -> x.hasAction("Fill") && x.hasAction("Check-ammo"));

                if (npc == null || ammoCrate == null) {
                    return;
                }

                boolean isWest = npc.getWorldLocation().getX() < ammoCrate.getWorldLocation().getX();
                TemporossWorkArea area = new TemporossWorkArea(npc.getWorldLocation(), isWest);
                log.info("Found work area: {}", area);
                workArea = area;
                return;
            }

            TileObject tether = workArea.getClosestTether();
            if (incomingWave)
            {
                if (!isTethered())
                {
                    if (tether == null)
                    {
                        log.warn("Can't find tether object");
                        ticks = 1;
                        return;
                    }

                    tether.interact("Tether");
                    ticks = 3;
                    return;
                }

                ticks = 2;
                return;
            }

            if (tether != null && Players.getLocal().getGraphic() == TemporossID.GRAPHIC_TETHERED)
            {
                tether.interact("Untether");
                ticks = 2;
                return;
            }

            WorldArea local = Players.getLocal().getWorldArea();
            TileObject fire = TileObjects.getNearest(local.toWorldPoint(), 41006);

            if(fire != null)
            {
                if (local.intersectsWith(fire.getWorldArea()))
                {
                    target = validMovements();
                    target.stream()
                            .filter(x -> !x.isInArea(fire.getWorldArea()))
                            .findFirst()
                            .ifPresent(Movement::walkTo);
                    ticks = 2;
                    return;
                }

            }

            NPC exitNpc = NPCs.getNearest(TemporossID.NPC_EXIT);
            if (exitNpc != null)
            {
                exitNpc.interact("Leave");
                workArea = null;
                ticks = 1;
                return;
            }

        }
        //LEAVE WHEN WIDGETS HEALTH = 0
        //cloud spawn = 41006
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        ChatMessageType type = event.getType();
        String message = event.getMessage();

        if (type == ChatMessageType.GAMEMESSAGE) {
            if (message.equals("<col=d30b0b>A colossal wave closes in...</col>")) {
                waves++;
                incomingWave = true;
            }

            if (message.contains("the rope keeps you securely") || message.contains("the wave slams into you")) {
                incomingWave = false;
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged e) {

        if (!inGame) {
            return;
        }

        final Player local = client.getLocalPlayer();
        if (e.getActor() != local) {
            return;
        }

        int animation = e.getActor().getAnimation();
        switch (animation) {
            case AnimationID.FISHING_BAREHAND:
            case AnimationID.FISHING_HARPOON:
            case AnimationID.FISHING_CRYSTAL_HARPOON:
            case AnimationID.FISHING_BARBTAIL_HARPOON:
            case AnimationID.FISHING_DRAGON_HARPOON_OR:
            case AnimationID.FISHING_DRAGON_HARPOON:
            case AnimationID.FISHING_TRAILBLAZER_HARPOON:
            case AnimationID.FISHING_INFERNAL_HARPOON:
                this.scriptState = State.FISHING;

                //3705 tether to pole
                //832 untether pole
        }
    }

    enum State {
        FISHING,
        TETHER,
    }

    private static int getRawFish() {
        return Inventory.getCount(TemporossID.ITEM_RAW_FISH);
    }

    private static int getCookedFish() {
        return Inventory.getCount(TemporossID.ITEM_COOKED_FISH);
    }

    private static int getAllFish() {
        return getRawFish() + getCookedFish();
    }

    private int getPhase() {
        return 1 + (waves / 3);
    }

    private static boolean isTethered() {
        int graphic = Players.getLocal().getGraphic();
        int anim = Players.getLocal().getAnimation();
        return anim != 832 && (graphic == TemporossID.GRAPHIC_TETHERED || graphic == TemporossID.GRAPHIC_TETHERING);
    }

    private List<WorldPoint> validMovements() {
        List<WorldPoint> points = new ArrayList<>(8);
        WorldArea local = Players.getLocal().getWorldArea();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {

                if (dx == 0 && dy == 0) {

                    continue;
                }
                if (local.canTravelInDirection(client, dx, dy)){
                    points.add(local.toWorldPoint().dx(dx).dy(dy));
                }
            }
        }
        return points;
    }
}