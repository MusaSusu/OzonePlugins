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
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.CameraController;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
                    LocalPoint target = new LocalPoint(
                            Players.getLocal().getLocalLocation().getX() + 256,
                            Players.getLocal().getLocalLocation().getY()
                    );
                    LocalPoint object = TileObjects.getNearest("Loot Chest").getLocalLocation();
                    System.out.println("Current: " + localToCanvas(client, object,0));
                    System.out.println("Projected: " + localToCanvasAtTile(client, object, target,0));
                }
        );
    }

    @Nullable
    public static Point localToCanvas(@Nonnull Client client, @Nonnull LocalPoint point, int plane)
    {
        return localToCanvas(client, point, plane, 0);
    }

    @Nullable
    public static Point localToCanvas(@Nonnull Client client, @Nonnull LocalPoint point, int plane, int zOffset)
    {
        final int tileHeight = Perspective.getTileHeight(client, point, plane);
        return localToCanvas(client, point.getX(), point.getY(), tileHeight - zOffset);
    }

    public static Point localToCanvas(@Nonnull Client client, int x, int y, int z)
    {
        if (x >= 128 && y >= 128 && x <= 13056 && y <= 13056)
        {
            x -= client.getCameraX();
            y -= client.getCameraY();
            z -= client.getCameraZ();

            final int cameraPitch = client.getCameraPitch();
            final int cameraYaw = client.getCameraYaw();

            final int pitchSin = SINE[cameraPitch];
            final int pitchCos = COSINE[cameraPitch];
            final int yawSin = SINE[cameraYaw];
            final int yawCos = COSINE[cameraYaw];

            final int
                    x1 = x * yawCos + y * yawSin >> 16,
                    y1 = y * yawCos - x * yawSin >> 16,
                    y2 = z * pitchCos - y1 * pitchSin >> 16,
                    z1 = y1 * pitchCos + z * pitchSin >> 16;

            if (z1 >= 50)
            {
                final int scale = client.getScale();
                final int pointX = client.getViewportWidth() / 2 + x1 * scale / z1;
                final int pointY = client.getViewportHeight() / 2 + y2 * scale / z1;
                return new Point(
                        pointX + client.getViewportXOffset(),
                        pointY + client.getViewportYOffset());
            }
        }

        return null;
    }

    public static Point localToCanvasAtTile(@Nonnull Client client,@Nonnull LocalPoint point,@Nonnull LocalPoint tile, int plane)
    {
        final int tileHeight = Perspective.getTileHeight(client, point, plane);
        int x = point.getX();
        int y = point.getY();
        int z = tileHeight;

        if (x >= 128 && y >= 128 && x <= 13056 && y <= 13056)
        {
            int cameraX = tile.getX() - (Players.getLocal().getLocalLocation().getX() - client.getCameraX());
            int cameraY = tile.getY() - (Players.getLocal().getLocalLocation().getY() - client.getCameraY());
            int cameraZ = client.getCameraZ();
            System.out.println(cameraX);
            System.out.println(cameraY);
            System.out.println(cameraZ);
            x -= cameraX;
            y -= cameraY;
            z -= cameraZ;

            final int cameraPitch = client.getCameraPitch();
            final int cameraYaw = client.getCameraYaw();

            final int pitchSin = SINE[cameraPitch];
            final int pitchCos = COSINE[cameraPitch];
            final int yawSin = SINE[cameraYaw];
            final int yawCos = COSINE[cameraYaw];

            final int
                    x1 = x * yawCos + y * yawSin >> 16,
                    y1 = y * yawCos - x * yawSin >> 16,
                    y2 = z * pitchCos - y1 * pitchSin >> 16,
                    z1 = y1 * pitchCos + z * pitchSin >> 16;

            if (z1 >= 50)
            {
                final int scale = client.getScale();
                final int pointX = client.getViewportWidth() / 2 + x1 * scale / z1;
                final int pointY = client.getViewportHeight() / 2 + y2 * scale / z1;
                return new Point(
                        pointX + client.getViewportXOffset(),
                        pointY + client.getViewportYOffset());
            }
        }
        return null;
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