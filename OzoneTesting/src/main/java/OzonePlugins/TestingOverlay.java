package OzonePlugins;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;

public class TestingOverlay extends Overlay {

    private Client client;

    @Inject
    private TestingOverlay(Client client)
    {
        this.client = client;
        setPriority(OverlayPriority.HIGHEST);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }
    @Override
    public Dimension render(Graphics2D graphics2D) {
        renderChatbox(graphics2D);
        renderInventory(graphics2D);
        renderMinimap(graphics2D);
        renderViewPort(graphics2D);
        return null;
    }

    private void renderChatbox(Graphics2D graphics2D)
    {
        Rectangle chatbox = client.getWidget(ComponentID.CHATBOX_FRAME).getParent().getBounds();
        Rectangle adjusted = new Rectangle(chatbox.x + 5,chatbox.y + 5, chatbox.width, chatbox.height);
        OverlayUtil.renderPolygon(graphics2D,adjusted, Color.BLUE);
    }

    private void renderInventory(Graphics2D graphics2D)
    {
        Rectangle inventoryBox;
        if (client.isResized())
        {
            inventoryBox = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_INTERFACE_CONTAINER).getParent().getBounds();
        }
        else
        {
            inventoryBox = client.getWidget(WidgetInfo.FIXED_VIEWPORT_INTERFACE_CONTAINER).getParent().getBounds();
        }
        Rectangle adjusted = new Rectangle(inventoryBox.x, inventoryBox.y, inventoryBox.width, inventoryBox.height);
        OverlayUtil.renderPolygon(graphics2D,adjusted, Color.BLUE);
    }

    private void renderMinimap(Graphics2D graphics2D)
    {
        Widget minimapDrawWidget;
        if (client.isResized()) {
            minimapDrawWidget = client.getWidget(10551318);
        } else {
            minimapDrawWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
        }

        Rectangle miniMapBox = minimapDrawWidget.getBounds();
        Rectangle adjusted = new Rectangle(miniMapBox.x, miniMapBox.y, miniMapBox.width, miniMapBox.height);
        OverlayUtil.renderPolygon(graphics2D,adjusted, Color.BLUE);
    }

    private void renderViewPort(Graphics2D graphics2D)
    {
        Widget viewPortWidget;
        if (client.isResized())
        {
            viewPortWidget = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX);
        }
        else
        {
            viewPortWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT);
        }

        Rectangle bounds = new Rectangle(0,0,viewPortWidget.getWidth(), (int) (viewPortWidget.getHeight() * 0.75));
        OverlayUtil.renderPolygon(graphics2D,bounds, Color.BLUE);
    }

    private void renderObject(Graphics2D graphics2D)
    {

    }

}
