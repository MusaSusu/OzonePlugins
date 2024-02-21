package ozone;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import net.unethicalite.api.game.Combat;
import org.pf4j.Extension;

import javax.inject.Inject;


@Extension
@PluginDescriptor(
        name = " Ozone AutoSpec Assist",
        description = "AutoSpec with Hotkey",
        tags = {"Ozone","Hotkey"}
)
@Slf4j
public class OzoneAutoSpec extends Plugin {

    @Inject
    private Client client;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OzoneAutoSpecConfig config;

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
            return;
        }
        else
        {
            Combat.toggleSpec();
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