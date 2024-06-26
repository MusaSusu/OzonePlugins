package OzonePlugins.components.invocations;

import OzonePlugins.data.RaidState;
import OzonePlugins.data.Invocation;
import lombok.Value;
import net.runelite.client.util.ColorUtil;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class InvocationPreset
{
	public static InvocationPreset parse(String serialized)
	{
		String[] parts = serialized.trim().split(";");
		String name = parts[0];

		if (parts.length != 2)
		{
			if (serialized.endsWith(";"))
			{
				return new InvocationPreset(parts[0], Collections.emptySet());
			}
			throw new IllegalArgumentException("Invalid format");
		}

		Set<Invocation> invocations = Arrays.stream(parts[1].split(","))
			.map(Invocation::valueOf)
			.collect(Collectors.toSet());

		return new InvocationPreset(name, invocations);
	}

	private final String name;
	private final Set<Invocation> invocations;

	public String serialize()
	{
		return name + ";" + invocations.stream().map(Invocation::name).collect(Collectors.joining(","));
	}

	public int getRaidLevel()
	{
		return invocations.stream()
			.mapToInt(Invocation::getRaidLevel)
			.sum();
	}

	public String toStringDecorated()
	{
		String nameColorTag = ColorUtil.colorTag(new Color(255, 152, 31));
		return nameColorTag + name + " "  + "(Lvl " + getRaidLevel() + ")" + ColorUtil.CLOSING_COLOR_TAG;
	}

}
