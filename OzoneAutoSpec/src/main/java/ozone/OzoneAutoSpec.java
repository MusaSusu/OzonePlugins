package ozone;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.function.Supplier;


@Extension
@PluginDescriptor(
        name = " Ozone AutoSpec Assist",
        description = "AutoSpec with Hotkey",
        tags = {"Ozone","Hotkey","Combat"}
)
@Slf4j
public class OzoneAutoSpec extends Plugin {

    @Inject
    private Client client;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OzoneAutoSpecConfig config;

    private static final Supplier<Widget> SPEC_BUTTON = () -> {
        return Widgets.get(593, 36);
    };

    @Provides
    private OzoneAutoSpecConfig getConfig(ConfigManager configManager){
        return configManager.getConfig(OzoneAutoSpecConfig.class);
    }

    @Override
    public void startUp()
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            keyManager.registerKeyListener(autoSpec);
        }
    }

    @Override
    public void shutDown()
    {
        keyManager.unregisterKeyListener(autoSpec);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            keyManager.unregisterKeyListener(autoSpec);
            return;
        }
        keyManager.registerKeyListener(autoSpec);
    }

    private void enableSpec()
    {
        if (Combat.isSpecEnabled())
        {
            if(!config.alwaysSpec()){
                Widget spec = (Widget) SPEC_BUTTON.get();
                if (spec!= null)
                {
                    spec.interact(0);
                }
            }
            return;
        }
        else
        {
            Widget spec = (Widget) SPEC_BUTTON.get();
            if (spec != null)
            {
                spec.interact(0);
            }
        }
    }
    private final HotkeyListener autoSpec = new HotkeyListener(() -> config.getSpecHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            enableSpec();
            System.out.println("Hotkeypressed");
        }
    };
}