package OzonePlugins.components.zebak;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.unethicalite.api.entities.*;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CrondisPuzzle implements PluginLifecycleComponent {

    private final Client client;
    private final EventBus eventBus;
    private CrondisState crondisState;

    private final static Point PALM_TREE_LOC = new Point(32, 48);
    private final static Point BARRIER_LOC = new Point(46, 49);
    private final static Point RUN_TILE_LOC = new Point(40,61);
    private final static Point WATERFALL1_LOC = new Point(40,74);
    private final static Point WATERFALL2_LOC = new Point(26,74);
    private final static Point WATER_CONTAINER_LOC = new Point(34,55);
    private LocalPoint start;

    private int ticks;


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
        eventBus.register(this);
        this.crondisState = CrondisState.START;
        this.start = TileObjects.getNearest("Exit").getLocalLocation();
        this.ticks = 0;
        TileOffsets.setStart(start,client);
    }

    @Override
    public void shutDown()
    {
        this.start = null;
        eventBus.unregister(this);
    }

    public void run(RaidState raidState, boolean isPaused)
    {
        if(ticks > 0)
        {
            ticks--;
            return;
        }
        if(raidState.getCurrentRoom() == RaidRoom.CRONDIS)
        {
            if(crondisState == null){
                System.out.println("crondisState is null");
                return;
            }
            if(start == null)
            {
                TileObject exit = TileObjects.getNearest("Exit");
                if (exit != null)
                {
                    this.start = TileObjects.getNearest("Exit").getLocalLocation();
                    TileOffsets.setStart(start,client);
                }
                else{
                    System.out.println("palm is null");
                    System.out.println(TileObjects.getNearest("Exit").getLocalLocation());//testing
                    return;
                }
            }
            if(isPaused){
                System.out.println(crondisState.getName()); //testing
                System.out.println(TileObjects.getNearest("Exit").getLocalLocation());
                return;
            }
            switch (crondisState){
                case START: {
                    if (!isOnTile(TileOffsets.START.getWorld()) && !Players.getLocal().isMoving()) {
                        Movement.walk(TileOffsets.START.getWorld());
                        break;
                    }
                    if (isOnTile(TileOffsets.START.getWorld()) ) {
                        TileObjects.getFirstAt(TileOffsets.GATE.getWorld(), "Barrier").interact("Quick-Pass");
                        this.crondisState = CrondisState.WATER_CONTAINER;
                        break;
                    }
                    break;
                }
                case WATER_CONTAINER: {
                    if( Players.getLocal().distanceTo(TileOffsets.WATER_CONTAINER.getWorld()) > 7 && !Inventory.contains("Water container"))
                    {
                        TileItem waterContainer = TileItems.getFirstAt(TileOffsets.WATER_CONTAINER.getWorld(),"Water container");
                        if(waterContainer == null)
                        {
                            System.out.println("waterContainer is null");
                            return;
                        }
                        System.out.println("Water container ID:" + waterContainer.getId());
                        waterContainer.interact("Take");
                        break;
                    }
                    if(Inventory.contains("Water container")){
                        Movement.walk(TileOffsets.FIRST_TILE.getWorld());
                        this.crondisState = CrondisState.FIRST_TILE;
                        ticks = 2;
                        break;
                    }
                    break;
                }
                case FIRST_TILE: {
                    if (Players.getLocal().distanceTo(TileOffsets.FIRST_TILE.getWorld()) <= 0) {
                        TileObject waterfall = TileObjects.getNearest(TileOffsets.FIRST_WATERFALL.getWorld(),"Waterfall");
                        if(waterfall == null){
                            System.out.println("waterfall is null");
                            break;
                        }
                        System.out.println("waterfall ID:" + waterfall.getId());
                        Inventory.getFirst("Water container").useOn(waterfall);
                        this.crondisState = CrondisState.WATERFALL;
                    }
                    break;
                }
                case WATERFALL: {
                    if(Players.getLocal().getAnimation() == 827)
                    {
                        this.crondisState = CrondisState.FIRST_PALM_RUN;
                        Movement.walk(TileOffsets.FIRST_TILE.getWorld());
                        break;
                    }
                    break;
                }
                case FIRST_PALM_RUN: {
                    if(!Players.getLocal().isMoving()){
                        Movement.walk(TileOffsets.FIRST_TILE.getWorld());
                        ticks = 4;
                        break;
                    }
                    if(!Players.getLocal().isInteracting())
                    {
                        NPC tree = NPCs.getNearest("Palm of Resourcefulness");
                        if(tree == null)
                        {
                            System.out.println("tree is null");
                            break;
                        }
                        Inventory.getFirst("Water container").useOn(tree);
                        ticks = 2;
                        this.crondisState = CrondisState.FIRST_PALM;
                        break;
                    }
                    break;
                }
                case FIRST_PALM: {
                    if(Players.getLocal().getAnimation() == 827)
                    {
                        this.crondisState = CrondisState.SECOND_TILE;
                        Movement.walk(TileOffsets.SECOND_TILE.getWorld());
                        break;
                    }
                    break;
                }
                case SECOND_TILE: {
                    if(!Players.getLocal().isMoving())
                    {
                        Movement.walk(TileOffsets.SECOND_TILE.getWorld());
                        ticks = 4;
                        break;
                    }
                    if(!Players.getLocal().isInteracting()) {
                        TileObject waterfall = TileObjects.getNearest(TileOffsets.SECOND_WATERFALL.getWorld(), "Waterfall");
                        if (waterfall == null) {
                            System.out.println("waterfall null");
                            break;
                        }
                        Inventory.getFirst("Water container").useOn(waterfall);
                        ticks = 4;
                        this.crondisState = CrondisState.SECOND_WATERFALL;
                        break;
                    }
                }
                case SECOND_WATERFALL: {
                    if(Players.getLocal().getAnimation() == 827) {
                        this.crondisState = CrondisState.SECOND_PALM_RUN;
                        Movement.walk(TileOffsets.SECOND_TILE.getWorld());
                        break;
                    }
                    break;
                }
                case SECOND_PALM_RUN: {
                    if(!Players.getLocal().isMoving()) {
                        Movement.walk(TileOffsets.SECOND_TILE.getWorld());
                        ticks = 4;
                        break;
                    }
                    if(!Players.getLocal().isInteracting())
                    {
                        NPC tree = NPCs.getNearest("Palm of Resourcefulness");
                        if(tree == null)
                        {
                            System.out.println("tree is null");
                            break;
                        }
                        System.out.println("Tree id" + tree.getId());
                        Inventory.getFirst("Water container").useOn(tree);
                        ticks = 4;
                        this.crondisState = CrondisState.SECOND_PALM;
                        break;
                    }
                }
                case SECOND_PALM:{
                    if(Players.getLocal().getAnimation() == 827)
                    {
                        this.crondisState = CrondisState.END;
                        break;
                    }
                    break;
                }
                case END:{
                    // TODO: if water is less than 100% then need to continue. Although this might just be a glitch.
                    TileObject entry = TileObjects.getFirstSurrounding(WorldPoint.fromScene(client.getLocalPlayer().getWorldView(),13,48,0),2,ObjectID.ENTRY_45397);
                    if(entry == null)
                    {
                        System.out.println("entry is null");
                    }
                    entry.interact("Quick-Enter");
                    System.out.println("done");
                }
                default: break;
            }
        }
    }

    private boolean isOnTile(WorldPoint tile) {
        return Players.getLocal().distanceTo(tile) <= 0;
    }
}
