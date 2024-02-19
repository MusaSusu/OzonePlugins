package Ozone.Wintertodt;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("Ozone-WintertodtConfig")
public interface WintertodtConfig extends Config {
    @ConfigSection(
            position = 0,
            keyName = "Ozone-Wintertodt",
            name = "Ozone Wintertodt Assistant",
            description = ""
    )
    String WintertodtConfig = "Wintertodt Config";
    @ConfigItem(
            position = 0,
            keyName = "Revive Pyromancer",
            name = "Revive Pyromancer",
            description = "Enable to auto revive pyromancer if close.",
            section = WintertodtConfig
    )
    default boolean isRevive() {return false;}

    @ConfigItem(
            position = 1,
            keyName = "Health threshold",
            name = "Health threshold",
            description = "Eat when you are below this health value",
            section = WintertodtConfig
    )
    default int getHPThresh() {return 20;}
}
