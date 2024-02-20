package ozone.pestcontrol;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.Players;
import org.pf4j.Extension;

import javax.inject.Inject;


@Extension
@PluginDescriptor(
        name = "Ozone Pest Control",
        description = "Pest Control Assistant"
)
@Slf4j
public class PestControl extends Plugin {
    @Inject
    private Client client;

    private static int PEST_CONTROL = 10537;
    private static int PEST_CONTROL_ISLAND = 52794;


    @Subscribe
    public void onGameTick(GameTick e)
    {

    }
    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        System.out.println(Players.getLocal().getWorldLocation().getRegionID());
    }

}