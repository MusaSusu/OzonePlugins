package OzonePlugins.components.zebak;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@RequiredArgsConstructor
public enum TileOffsets {
    START(-3,1),
    GATE(-4,1),
    WATER_CONTAINER(-16, 7),
    FIRST_TILE(-10,13),
    FIRST_WATERFALL(-10,25),
    PALM_TREE(0,0),
    SECOND_TILE(-23,13),
    SECOND_WATERFALL(-23,25)
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
            LocalPoint local = new LocalPoint(start.getX() - 192 + val.getXOffset(), start.getY() + val.getYOffset(), -1);
            val.world = WorldPoint.fromLocal(client,local);
        }
    }
}
