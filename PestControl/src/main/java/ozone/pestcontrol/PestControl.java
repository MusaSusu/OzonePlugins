package ozone.pestcontrol;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.widgets.Prayers;
import org.pf4j.Extension;

import javax.inject.Inject;

@Extension
@PluginDescriptor(
        name = "Ozone Pest Control",
        description = "Pest Control Assistant",
        tags = {"Minigame","Ozone"}
)
@Slf4j
public class PestControl extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private PestControlConfig config;
    @Provides
    private PestControlConfig getConfig(ConfigManager configManager){
        return configManager.getConfig(PestControlConfig.class);
    }
    private static int PEST_CONTROL = 10537;
    private static WorldArea PEST_CONTROL_ISLAND = new WorldArea(
            new WorldPoint(2624,2560,0),
            new WorldPoint(2687,2623,0));
    private boolean inGame = false;
    private int tick = 0;
    private boolean shouldCross;
    @Subscribe
    public void onGameTick(GameTick e) {
        if (tick > 0)
        {
            tick--;
            return;
        }
        if (shouldCross)
        {
            System.out.println("crossing");
            TileObjects.getNearest("Gangplank").interact("Cross");
            shouldCross = false;
            return;
        }
        if (client.getWidget(ComponentID.PEST_CONTROL_BLUE_SHIELD) == null)
        {
            if (inGame)
            {
                log.debug("Pest control game has ended");
                System.out.println("Pest control game has ended");
                shouldCross = true;
                tick = 1;
                inGame = false;
            }

            return;

        }
        if (!inGame)
        {
            log.debug("Pest control game has started");
            if (config.enablePrayer()){
                Prayers.toggleQuickPrayer(true);
            }
            inGame = true;
        }
    }
    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        System.out.println(Players.getLocal().getWorldLocation().getRegionID());
    }
}