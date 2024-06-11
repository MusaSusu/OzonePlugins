package OzonePlugins.components.zebak;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class ZebakManager implements PluginLifecycleComponent {

    @Override
    public boolean isEnabled(RaidState raidState) {
        if (!raidState.isInRaid())
        {
            return false;
        }
        return raidState.getCurrentRoom() == RaidRoom.ZEBAK;
    }
    @Override
    public void startUp()
    {
    }

    @Override
    public void shutDown()
    {

    }

    public void run()
    {
        System.out.println("running");
    }
}
