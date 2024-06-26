package ozone.zulrah;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import ozone.zulrah.config.MagePrayer;
import ozone.zulrah.config.RangePrayer;

@ConfigGroup("ozone zulrah")
public interface OzoneZulrahConfig extends Config
{
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
			keyName = "magePrayer",
			name = "Magic prayer",
			description = "Mage prayer setting",
			position = 2
	)
	default MagePrayer magePrayer()
	{
		return MagePrayer.MYSTIC_MIGHT;
	}

	@ConfigItem(
			keyName = "rangePrayer",
			name = "Range prayer",
			description = "Range prayer setting",
			position = 3
	)
	default RangePrayer rangePrayer()
	{
		return RangePrayer.EAGLE_EYE;
	}

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
			keyName = "Rotate camera",
			name = "Camera",
			description = "Enable for camera rotation"
	)
	default boolean shouldRotateCamera(){return false;}

}
