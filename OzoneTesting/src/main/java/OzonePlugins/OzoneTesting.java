package OzonePlugins;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.CameraController;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.Rectangle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static net.runelite.api.Perspective.COSINE;
import static net.runelite.api.Perspective.SINE;


@Extension
@PluginDescriptor(
        name = " Ozone Testing",
        description = "Helps with testing",
        tags = {"Ozone"}
)
@Slf4j
public class OzoneTesting extends Plugin {
    @Inject
    private Client client;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OzoneTestingConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private TestingOverlay testingOverlay;

    private ExecutorService executor;

    private Rectangle bounds;

    @Inject
    private CameraController cameraController;

    private static final Supplier<Widget> SPEC_BUTTON = () -> {
        return Widgets.get(593, 36);
    };

    @Provides
    private OzoneTestingConfig getConfig(ConfigManager configManager){
        return configManager.getConfig(OzoneTestingConfig.class);
    }

    @Override
    public void startUp()
    {
        overlayManager.add(testingOverlay);
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            keyManager.registerKeyListener(autoSpec);
        }
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void shutDown()
    {
        overlayManager.remove(testingOverlay);
        keyManager.unregisterKeyListener(autoSpec);
        executor.shutdownNow();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            keyManager.unregisterKeyListener(autoSpec);
            return;
        }
        keyManager.registerKeyListener(autoSpec);
    }

    private void enableSpec()
    {
        executor.execute(()->
                {
                    LocalPoint object = TileObjects.getNearest("Loot Chest").getLocalLocation();
                    cameraController.alignToNorth(object);
                }
        );
    }

    private void shouldRotateCamera(LocalPoint dest)
    {
        Widget viewPortWidget;
        if (client.isResized()) {
            viewPortWidget = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX);
        }
        else
        {
            viewPortWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT);
        }
        this.bounds = viewPortWidget.getBounds();

        System.out.println(bounds);
        System.out.println(Perspective.localToCanvas(client,dest,0).getAwtPoint());
        System.out.println(this.bounds.contains(Perspective.localToCanvas(client,dest,0).getAwtPoint()));

        if( !this.bounds.contains(Perspective.localToCanvas(client,dest,0).getAwtPoint()))
        {
            cameraController.alignToNorth(dest);
        }
    }
    private final HotkeyListener autoSpec = new HotkeyListener(() -> config.getSpecHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            enableSpec();
            System.out.println("Hotkeypressed");
        }
    };
}