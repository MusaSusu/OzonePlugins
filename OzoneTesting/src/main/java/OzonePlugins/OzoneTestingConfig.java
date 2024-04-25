package OzonePlugins;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("OzoneTestingConfig")
public interface OzoneTestingConfig extends Config {

    @ConfigItem(
            keyName = "Spec Hotkey",
            name = "Spec Hotkey",
            description = "Set hotkey to autospec"
    )
    default Keybind getSpecHotkey()
    {
        return Keybind.NOT_SET;
    }

}
