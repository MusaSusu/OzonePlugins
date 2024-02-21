package ozone.pestcontrol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Ozone Pest Control Config")
public interface OzonePestControlConfig extends Config {
    @ConfigItem(
            keyName = "Quickprayers",
            name = "Enable quickprayers",
            description = "Auto enable quickprayers on game start"
    )
    default boolean enablePrayer(){return false;}
}
