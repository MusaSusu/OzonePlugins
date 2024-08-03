package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.unethicalite.api.entities.TileObjects;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ScabarasManager implements PluginLifecycleComponent {

    private final Client client;
    private final EventBus eventBus;

    private ScabarasState scabarasState;
    private int gameTicks = 0;
    private boolean isFirstPuzzle = true;

    @Inject
    private AdditionPuzzleSolver additionPuzzleSolver;
    @Inject
    private LayoutConfigurer layoutConfigurer;

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
        this.scabarasState = ScabarasState.START;
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);
    }

    public void run(RaidState raidState, boolean isPaused) {
        gameTicks++;
        if(isPaused)
        {
            return;
        }
        else{

            switch (scabarasState) {
                case START: {
                    LocalPoint flame = layoutConfigurer.getFlameLocation();
                    if (flame == null){
                        break;
                    }
                    else {
                        LocalPoint destination = new LocalPoint(flame.getX() + 128, flame.getY());
                        if(!client.getLocalPlayer().isMoving() && client.getLocalPlayer().getLocalLocation() != destination)
                        {
                            TileObjects.getNearest(WorldPoint.fromLocal(client,flame), ObjectID.BARRIER_45135).interact("Quick-Pass");
                            System.out.println("gameTick on click: " +  gameTicks);
                            break;
                        }
                        if(client.getLocalPlayer().getLocalLocation() == destination)
                        {
                            System.out.println("gameTick on arrive: " +  gameTicks);
                            this.scabarasState = layoutConfigurer.getCurrentPuzzle(isFirstPuzzle);
                        }
                        break;
                    }
                }
                case ADDITION_PUZZLE:
                    additionPuzzleSolver.run();
                    break;
                case LIGHT_PUZZLE:
                case SEQUENCE_PUZZLE:

                case MATCHING_PUZZLE:

                case END: {

                }
            }
        }
        System.out.println(scabarasState.getName());
    }

    private boolean checkTaskComplete(LocalPoint destination)
    {
        if (client.getLocalPlayer().getLocalLocation() != destination)
        {
            return false;
        }
        return true;
    }

}
