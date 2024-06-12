package OzonePlugins.components.zebak;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileItems;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;


import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class ZebakManager implements PluginLifecycleComponent {

    @Inject
    private Client client;
    private EventBus eventBus;
    private CrondisState crondisState;

    private LocalPoint start;


    @Override
    public boolean isEnabled(RaidState raidState) {
        if (!raidState.isInRaid())
        {
            return false;
        }
        return raidState.getCurrentRoom() == RaidRoom.CRONDIS | raidState.getCurrentRoom() == RaidRoom.ZEBAK;
    }
    @Override
    public void startUp()
    {
        this.crondisState = CrondisState.START;
        this.start = Players.getLocal().getLocalLocation();
        TileOffsets.setStart(start,client);
        //eventBus.register(this);
    }

    @Override
    public void shutDown()
    {
        this.start = null;
        //eventBus.unregister(this);
    }

    public void run(RaidState raidState, boolean isPaused)
    {
        if(raidState.getCurrentRoom() == RaidRoom.CRONDIS)
        {
            if(crondisState == null){
                System.out.println("crondisState is null");
                return;
            }
            if(start == null)
            {
                this.start = Players.getLocal().getLocalLocation();
                TileOffsets.setStart(start,client);
            }
            if(isPaused){
                System.out.println(crondisState.getName());
                TileItem item = TileItems.getFirstAt(TileOffsets.WATER_CONTAINER.getWorld(),"Water container");
                if (item != null)
                {
                    System.out.println("water container:" + item.getName());
                }
                return;
            }
            switch (crondisState){
                case START: {
                    if (!isOnTile(TileOffsets.START.getWorld()) && !Players.getLocal().isMoving()) {
                        Movement.walk(TileOffsets.START.getWorld());
                        break;
                    }
                    if (isOnTile(TileOffsets.START.getWorld()) ) {
                        TileObjects.getNearest(TileOffsets.START.getWorld(), "Barrier").interact("Quick-Pass");
                        break;
                    }
                    if(isOnTile(TileOffsets.GATE.getWorld()))
                    {
                        TileItems.getFirstAt(TileOffsets.WATER_CONTAINER.getWorld(),"Water container").interact("Take");
                        this.crondisState = CrondisState.WATER_CONTAINER;
                    }
                    break;
                }
                case WATER_CONTAINER: {
                    if(!Players.getLocal().isMoving() && !Inventory.contains("Water container"))
                    {
                        TileItems.getFirstAt(TileOffsets.WATER_CONTAINER.getWorld(),"Water container").interact("Take");
                        break;
                    }
                    if(Inventory.contains("Water container")){
                        Movement.walkTo(TileOffsets.FIRST_TILE.getWorld());
                        this.crondisState = CrondisState.FIRST_TILE;
                        break;
                    }
                }
                case FIRST_TILE: {
                    if (Players.getLocal().distanceTo(TileOffsets.FIRST_TILE.getWorld()) > 1) {
                        break;
                    } else {
                        Inventory.getFirst("Water container").useOn(TileObjects.getFirstAt(TileOffsets.FIRST_WATERFALL.getWorld(), "Waterfall"));
                        this.crondisState = CrondisState.WATERFALL;
                        break;
                    }
                }
                case WATERFALL: {
                    System.out.println("first palm");
                }
                default: break;

            }
        }
    }

    private boolean isOnTile(WorldPoint tile) {
        return Players.getLocal().distanceTo(tile) <= 0;
    }
}
