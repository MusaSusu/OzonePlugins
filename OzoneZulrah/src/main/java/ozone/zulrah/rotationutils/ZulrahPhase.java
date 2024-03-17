package ozone.zulrah.rotationutils;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public final class ZulrahPhase
{
	@NonNull
	private final ZulrahNpc zulrahNpc;

	@NonNull
	@Getter
	private final ZulrahAttributes attributes;
	public String toString()
	{
		ZulrahNpc zulrahNpc = getZulrahNpc();
		return "ZulrahPhase(zulrahNpc=" + zulrahNpc + ", attributes=" + getAttributes() + ")";
	}
}
