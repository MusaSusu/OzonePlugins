package ozone.zulrah.rotationutils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import ozone.zulrah.data.StandLocation;
import ozone.zulrah.OzoneZulrahPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public final class ZulrahAttributes
{
	@Nonnull
	private final StandLocation standLocation;
	@Nullable
	private final StandLocation stallLocation;
	@Nullable
	private final Prayer prayer;
	@Getter
	private final int phaseTicks;
	@Getter
	private final int phaseAttacks;
	@Getter
	private final int ticksToMove;


	public Optional<StandLocation> getCurrentDynamicStandLocation() {
		switch (getStandLocation()) {
			case NORTHEAST_TOP:
				return OzoneZulrahPlugin.isFlipStandLocation() ? Optional.of(StandLocation.NORTHEAST_BOTTOM) : Optional.of(getStandLocation());
			case WEST:
				return OzoneZulrahPlugin.isFlipStandLocation() ? Optional.of(StandLocation.NORTHWEST_BOTTOM) : Optional.of(getStandLocation());
			default:
				return Optional.of(getStandLocation());
		}
	}


	@Nonnull
	public StandLocation getStandLocation()
	{
		return this.standLocation;
	}

	@Nullable
	public StandLocation getStallLocation()
	{
		return this.stallLocation;
	}

	@Nullable
	public Prayer getPrayer()
	{
		return this.prayer;
	}

	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		else if (!(o instanceof ZulrahAttributes))
		{
			return false;
		}
		else
		{
			ZulrahAttributes other = (ZulrahAttributes) o;
			Object this$standLocation = getStandLocation();
			Object other$standLocation = other.getStandLocation();
			if (this$standLocation == null)
			{
				if (other$standLocation != null)
				{
					return false;
				}
			}
			else if (!this$standLocation.equals(other$standLocation))
			{
				return false;
			}

			label41:
			{
				Object this$stallLocation = getStallLocation();
				Object other$stallLocation = other.getStallLocation();
				if (this$stallLocation == null)
				{
					if (other$stallLocation == null)
					{
						break label41;
					}
				}
				else if (this$stallLocation.equals(other$stallLocation))
				{
					break label41;
				}

				return false;
			}

			Object this$prayer = getPrayer();
			Object other$prayer = other.getPrayer();
			if (this$prayer == null)
			{
				if (other$prayer != null)
				{
					return false;
				}
			}
			else if (!this$prayer.equals(other$prayer))
			{
				return false;
			}

			if (getPhaseTicks() != other.getPhaseTicks())
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
		byte PRIME = 59;
		int result = 1;
		Object $standLocation = getStandLocation();
		result = result * PRIME + $standLocation.hashCode();
		Object $stallLocation = getStallLocation();
		result = result * PRIME + ($stallLocation == null ? 43 : $stallLocation.hashCode());
		Object $prayer = getPrayer();
		result = result * PRIME + ($prayer == null ? 43 : $prayer.hashCode());
		result = result * PRIME + getPhaseTicks();
		return result;
	}

	public String toString()
	{
		StandLocation standLocation = getStandLocation();
		return "ZulrahAttributes(standLocation=" + standLocation + ", stallLocation=" + getStallLocation() + ", prayer=" + getPrayer() + ", phaseTicks=" + getPhaseTicks() + ")";
	}
}
