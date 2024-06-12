package OzonePlugins.components.zebak;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@RequiredArgsConstructor
public enum TileOffsets {
    START(-3,0),
    GATE(-5,0),
    WATER_CONTAINER(-16,+6),
    FIRST_TILE(-10,+12),
    FIRST_WATERFALL(-6,+23),
    PALM_TREE(-18,+1),
    SECOND_WATERFALL(-23,+24)
    ;

    private final int xOffset;
    private final int yOffset;

    @Getter
    private WorldPoint world;

    private int getXOffset(){
        return this.xOffset * 128;
    }
    private int getYOffset(){
        return this.yOffset * 128;
    }


    public static void setStart(LocalPoint start, Client client){
        for (TileOffsets val : TileOffsets.values())
        {
            LocalPoint local = new LocalPoint(start.getX() + val.getXOffset(), start.getY() + val.getYOffset(), -1);
            val.world = WorldPoint.fromLocal(client,local);
        }
    }
}
