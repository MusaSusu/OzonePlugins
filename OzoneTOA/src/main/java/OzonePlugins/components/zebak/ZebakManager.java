package OzonePlugins.components.zebak;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ZebakManager implements PluginLifecycleComponent {

    private final Client client;
    private final EventBus eventBus;
    private final CrondisPuzzle crondisPuzzle;

    @Override
    public boolean isEnabled(RaidState raidState) {
        return raidState.isInRaid() && (raidState.getCurrentRoom().equals(RaidRoom.ZEBAK) || raidState.getCurrentRoom().equals(RaidRoom.CRONDIS));
    }

    @Override
    public void startUp() {
        eventBus.register(this)
        ;
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);
    }

    public void run(RaidState raidState,boolean isPaused)
    {
        if(raidState.getCurrentRoom().equals(RaidRoom.CRONDIS))
        {
            crondisPuzzle.run(raidState,isPaused);
            return;
        }
    }
}
