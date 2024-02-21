package ozone.pestcontrol;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
public class OzonePestControl extends Plugin {
    @Inject
    private Client client;
    @Inject
    private OzonePestControlConfig config;
    @Provides
    private OzonePestControlConfig getConfig(ConfigManager configManager){
        return configManager.getConfig(OzonePestControlConfig.class);
    }
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
}