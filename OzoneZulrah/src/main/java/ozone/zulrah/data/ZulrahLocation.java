package ozone.zulrah.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.LocalPoint;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum ZulrahLocation
{
	NORTH(6720, 7616),
	EAST(8000, 7360),
	SOUTH(6720, 6208),
	WEST(5440, 7360);

	private final int localX;
	private final int localY;

	public LocalPoint toLocalPoint()
	{
		return new LocalPoint(this.localX, this.localY);
	}

	public static ZulrahLocation valueOf(final LocalPoint localPoint)
	{
		for (final ZulrahLocation loc : values())
		{
			if (loc.toLocalPoint().equals(localPoint))
			{
				return loc;
			}
		}
		return null;
	}

}