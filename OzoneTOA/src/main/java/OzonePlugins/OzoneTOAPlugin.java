package OzonePlugins;


import OzonePlugins.components.kephri.ScabarasManager;
import OzonePlugins.components.zebak.ZebakManager;
import OzonePlugins.data.*;
import OzonePlugins.modules.ComponentManager;
import OzonePlugins.modules.TOAModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = " Ozone TOA",
        description = "Completes TOA",
        tags = {"Ozone","Combat"}
)
public class OzoneTOAPlugin extends Plugin {

    @Inject
    private Client client;
    @Inject
    private OzoneTOAConfig config;
    @Inject
    private KeyManager keyManager;

    private ComponentManager componentManager = null;

    private ZebakManager zebakManager = null;
    private ScabarasManager scabarasManager = null;

    private boolean isPaused = true;
    private RaidState raidState;
    @Provides
    @Singleton
    OzoneTOAConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OzoneTOAConfig.class);
    }

    @Override
    public void configure(Binder binder)
    {
        binder.install(new TOAModule());
    }

    @Override
    protected void startUp() {
        List<String> rangeGearNames = Arrays.stream(config.rangeGearNames().split(","))
                .collect(Collectors.toList());
        List<String> mageGearNames = Arrays.stream(config.mageGearNames().split(","))
                .collect(Collectors.toList());
        List<String> meleeGearNames = Arrays.stream(config.meleeGearNames().split(","))
                .collect(Collectors.toList());

        GearTypes.setMagePhaseGear(new GearSetup(rangeGearNames));
        GearTypes.setRangedMeleePhaseGear(new GearSetup(mageGearNames));
        GearTypes.setMeleePhaseGear(new GearSetup(meleeGearNames));

        keyManager.registerKeyListener(scriptControl);

        if (componentManager == null)
        {
            componentManager = injector.getInstance(ComponentManager.class);
        }
        componentManager.onPluginStart();

    }

    @Override
    protected void shutDown()
    {
        keyManager.unregisterKeyListener(scriptControl);
        componentManager.onPluginStop();
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if(raidState.isInLobby())
        {
            return;
        }
        if(raidState.isInRaid())
        {
            if(raidState.getCurrentRoom() == RaidRoom.ZEBAK || raidState.getCurrentRoom() == RaidRoom.CRONDIS)
            {
                if (zebakManager == null)
                {
                    this.zebakManager = injector.getInstance(ZebakManager.class);
                }
                zebakManager.run(raidState,isPaused);
            }
            else if(raidState.getCurrentRoom() == RaidRoom.SCABARAS)
            {
                if (scabarasManager == null)
                {
                    this.scabarasManager = injector.getInstance(ScabarasManager.class);
                }
                scabarasManager.run(raidState,isPaused);
            }
        }
    }

    @Subscribe
    private void onRaidStateChanged(RaidStateChanged raidState)
    {
        this.raidState = raidState.getNewState();
        System.out.println("Raid Stata Change: + " + raidState.getNewState());
    }

    private final HotkeyListener scriptControl = new HotkeyListener(() -> config.getHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            isPaused = !isPaused;
        }
    };

}