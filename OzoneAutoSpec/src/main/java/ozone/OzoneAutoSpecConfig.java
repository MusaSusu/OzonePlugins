package ozone;


import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.util.Locale;

@ConfigGroup("OzoneAutoSpecConfig")
public interface OzoneAutoSpecConfig extends Config {

    @ConfigItem(
            keyName = "Spec Hotkey",
            name = "Spec Hotkey",
            description = "Set hotkey to autospec"
    )
    default Keybind getSpecHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "Always spec",
            name = "Always spec",
            description = "Enable to always spec. Else toggles spec on/off"
    )
    default boolean alwaysSpec(){return false;}
}
