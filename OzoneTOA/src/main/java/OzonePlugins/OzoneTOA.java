package OzonePlugins;


import OzonePlugins.data.GearSetup;
import OzonePlugins.data.GearTypes;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = " Ozone TOA",
        description = "Completes TOA",
        tags = {"Ozone","Combat"}
)
public class OzoneTOA extends Plugin {

    @Inject
    private Client client;
    @Inject
    private OzoneTOAConfig config;

    @Provides
    OzoneTOAConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OzoneTOAConfig.class);
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
    }

    @Override
    protected void shutDown()
    {

    }
}