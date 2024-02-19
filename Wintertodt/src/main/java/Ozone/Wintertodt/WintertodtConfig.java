package Ozone.Wintertodt;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("Ozone-WintertodtConfig")
public interface WintertodtConfig extends Config {
    @ConfigItem(
            position = 0,
            keyName = "Revive Pyromancer",
            name = "Revive Pyromancer",
            description = "Enable to auto revive pyromancer if close."
    )
    default boolean isRevive() {return false;}
    @ConfigItem(
            position = 1,
            keyName = "Health threshold",
            name = "Health threshold",
            description = "Eat when you are below this health value"
    )
    default int getHPThresh() {return 20;}
}
