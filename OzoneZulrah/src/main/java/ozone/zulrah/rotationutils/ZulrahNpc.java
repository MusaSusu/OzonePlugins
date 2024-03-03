package ozone.zulrah.rotationutils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NPC;
import ozone.zulrah.data.ZulrahLocation;
import ozone.zulrah.data.ZulrahType;

import javax.annotation.Nonnull;

@Getter
@RequiredArgsConstructor
public final class ZulrahNpc
{
	@Nonnull
	private final ZulrahType type;
	@Nonnull
	private final ZulrahLocation zulrahLocation;
	private final boolean jad;

	public static ZulrahNpc valueOf(NPC zulrah, boolean jad)
	{
		return new ZulrahNpc(ZulrahType.valueOf(zulrah.getId()), ZulrahLocation.valueOf(zulrah.getLocalLocation()), jad);
	}

	@Nonnull
	public ZulrahType getType()
	{
		return type;
	}

	@Nonnull
	public ZulrahLocation getZulrahLocation()
	{
		return zulrahLocation;
	}

	public boolean isJad()
	{
		return jad;
	}

	public String toString()
	{
		ZulrahType type = getType();
		return "ZulrahNpc(type=" + type + ", zulrahLocation=" + getZulrahLocation() + ", jad=" + isJad() + ")";
	}

	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		else if (!(o instanceof ZulrahNpc))
		{
			return false;
		}
		else
		{
			ZulrahNpc other = (ZulrahNpc) o;
			Object this$type = getType();
			Object other$type = other.getType();
			if (this$type == null)
			{
				if (other$type != null)
				{
					return false;
				}
			}
			else if (!this$type.equals(other$type))
			{
				return false;
			}

			label29:
			{
				Object this$zulrahLocation = getZulrahLocation();
				Object other$zulrahLocation = other.getZulrahLocation();
				if (this$zulrahLocation == null)
				{
					if (other$zulrahLocation == null)
					{
						break label29;
					}
				}
				else if (this$zulrahLocation.equals(other$zulrahLocation))
				{
					break label29;
				}

				return false;
			}

			if (isJad() != other.isJad())
			{
				return false;
			}
			else
			{
				return true;
			}
		}
	}

	public int hashCode()
	{
		int PRIME = 59;
		int result = 1;
		Object $type = getType();
		result = result * PRIME + ($type == null ? 43 : $type.hashCode());
		Object $zulrahLocation = getZulrahLocation();
		result = result * PRIME + ($zulrahLocation == null ? 43 : $zulrahLocation.hashCode());
		result = result * PRIME + (isJad() ? 79 : 97);
		return result;
	}
}
