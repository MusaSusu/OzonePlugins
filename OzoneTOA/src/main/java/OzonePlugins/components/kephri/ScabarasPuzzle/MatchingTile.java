package OzonePlugins.components.kephri.ScabarasPuzzle;

import lombok.Data;
import net.runelite.api.coords.LocalPoint;

import java.awt.Color;

@Data
public class MatchingTile
{

    private final int flipId;
    private final int unflippedId;
    private LocalPoint localPointN;
    private LocalPoint localPointS;
    private boolean matched;


}