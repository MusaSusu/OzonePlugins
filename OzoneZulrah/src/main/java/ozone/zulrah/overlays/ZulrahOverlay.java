package ozone.zulrah.overlays;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import ozone.zulrah.OzoneTasksController;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class ZulrahOverlay extends OverlayPanel {
        private final Client client;
        private final OzoneTasksController ozoneTasksController;
        @Inject
        private ZulrahOverlay(Client client,OzoneTasksController ozoneTasksController)
        {
            this.client = client;
            this.ozoneTasksController = ozoneTasksController;
            setPriority(OverlayPriority.HIGHEST);
            setPosition(OverlayPosition.BOTTOM_RIGHT);
            setLayer(OverlayLayer.ALWAYS_ON_TOP);
        }
        @Override
        public Dimension render(Graphics2D graphics)
        {
            panelComponent.setPreferredSize(new Dimension(200,100));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Ozone Zulrah")
                    .color(Color.white)
                    .build());

            String statusText;
            boolean textColor;
            if (ozoneTasksController.isRunning())
            {
                statusText = ozoneTasksController.getCurrentTask();
                textColor = false;
            }
            else
            {
                statusText = "Not running";
                textColor = true;
            }

            TextComponent status = new TextComponent();
            status.setText(statusText);
            status.setColor(textColor ? Color.red : Color.GREEN);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Task:")
                    .leftColor(Color.white)
                    .right(statusText)
                    .rightColor(textColor ? Color.red : Color.GREEN)
                    .build());
            return super.render(graphics);
        }
}
