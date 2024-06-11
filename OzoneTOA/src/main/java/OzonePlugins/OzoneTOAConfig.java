package OzonePlugins;

import OzonePlugins.config.MeleePrayer;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import OzonePlugins.config.MagePrayer;
import OzonePlugins.config.RangePrayer;
import net.runelite.client.config.Keybind;

@ConfigGroup(OzoneTOAConfig.CONFIG_GROUP)
public interface OzoneTOAConfig extends Config {

    String CONFIG_GROUP = "ozonetoascript";

    @ConfigItem(
            keyName = "rangeGear",
            name = "Ranged gear names",
            description = "",
            position = 0
    )
    default String rangeGearNames()
    {
        return "Ancient d'hide body,Ancient chaps,Infinity boots,Toxic blowpipe,Ava's assembler";
    }

    @ConfigItem(
            keyName = "mageGear",
            name = "Mage gear names",
            description = "",
            position = 1
    )
    default String mageGearNames()
    {
        return "Ahrim's robetop,Ahrim's robeskirt,Trident of the swamp,Book of darkness";
    }

    @ConfigItem(
            keyName = "mageGear",
            name = "Mage gear names",
            description = "",
            position = 2
    )
    default String meleeGearNames()
    {
        return "Torva full helm,Bandos chestplate,Torva platelegs,Amulet of torture,Dragon defender,Osmumten's fang (or),Barrows gloves";
    }

    @ConfigItem(
            keyName = "magePrayer",
            name = "Magic prayer",
            description = "Mage prayer setting",
            position = 3
    )
    default MagePrayer magePrayer()
    {
        return MagePrayer.MYSTIC_MIGHT;
    }

    @ConfigItem(
            keyName = "rangePrayer",
            name = "Range prayer",
            description = "Range prayer setting",
            position = 4
    )
    default RangePrayer rangePrayer()
    {
        return RangePrayer.EAGLE_EYE;
    }

    @ConfigItem(
            keyName = "meleePrayer",
            name = "Melee prayer",
            description = "Melee prayer setting",
            position = 5
    )
    default MeleePrayer meleePrayer()
    {
        return MeleePrayer.PIETY;
    }

    @ConfigItem(
            keyName = "Script Hotkey",
            name = "Script Hotkey",
            description = "Set hotkey to start/pause script"
    )
    default Keybind getHotkey()
    {
        return Keybind.NOT_SET;
    }
}
