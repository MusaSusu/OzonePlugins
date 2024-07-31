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
                        TileObjects.getNearest(WorldPoint.fromLocal(client,flame), ObjectID.BARRIER_45135).interact("Quick-Pass");
                        this.scabarasState = ScabarasState.END;
                         break;
                    }
                }
                case END: {

                }
            }
        }

    }

}
