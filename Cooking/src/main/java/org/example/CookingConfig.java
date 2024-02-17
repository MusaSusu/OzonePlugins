package org.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("Ozone-CookingConfig")
public interface CookingConfig extends Config {
    @ConfigSection(
            position = 0,
            keyName = "One Click Cooking",
            name = "One Click Cooking",
            description = ""
    )
    String CookingConfig = "Cooking Config";
    @ConfigItem(
            position = 0,
            keyName = "foods",
            name = "Foods to cook",
            description = "Format is as follows: Raw shrimp, Raw monkfish",
            section = CookingConfig
    )
    default String getFoods()
    {
        return "Raw shrimp";
    }
}
