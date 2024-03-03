package ozone.zulrah.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum ZulrahType
{
	RANGE("Range", 2042, Skill.RANGED, Color.YELLOW),
	MELEE("Melee", 2043, Skill.ATTACK, Color.RED),
	MAGIC("Magic", 2044, Skill.MAGIC, Color.CYAN);

	private static final Logger log;
	private final String name;
	private final int npcId;
	private final Skill skill;
	private final Color color;
	@Setter
	private GearSetup setup;

	public static void setRangedMeleePhaseGear(GearSetup gearSetup)
	{
		RANGE.setSetup(gearSetup);
		MELEE.setSetup(gearSetup);
	}

	public static void setMagePhaseGear(GearSetup gearSetup)
	{
		MAGIC.setSetup(gearSetup);
	}

	public static ZulrahType valueOf(final int npcId)
	{
		switch (npcId)
		{
			case 2042:
			{
				return ZulrahType.RANGE;
			}
			case 2043:
			{
				return ZulrahType.MELEE;
			}
			case 2044:
			{
				return ZulrahType.MAGIC;
			}
			default:
			{
				return null;
			}
		}
	}

	public Color getColorWithAlpha(final int alpha)
	{
		return new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha);
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	static
	{
		log = LoggerFactory.getLogger(ZulrahType.class);
	}
}
