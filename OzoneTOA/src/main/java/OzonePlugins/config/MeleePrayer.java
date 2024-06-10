package OzonePlugins.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;

@RequiredArgsConstructor
public enum MeleePrayer
{
    PIETY(Prayer.PIETY),
    CHIVALRY(Prayer.CHIVALRY)
    ;
    @Getter
    private final Prayer prayer;
}
