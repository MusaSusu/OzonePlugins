package OzonePlugins;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.widgets.Widgets;

import javax.inject.Inject;
import javax.inject.Singleton;

@PluginDescriptor(
        name = "Ozone Utils",
        hidden = true
)
@Singleton
public class OzoneUtils extends Plugin {
    @Inject
    private Client client;

    private boolean isVisible(LocalPoint p)
    {
        Point chatbox = Widgets.get(WidgetInfo.CHATBOX).getCanvasLocation();
        return false;
    }
}