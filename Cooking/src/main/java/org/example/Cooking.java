package org.example;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;


@Extension
@PluginDescriptor(
        name = " Ozone Cooking Assist",
        description = "Helps with cooking"
)
public class Cooking extends Plugin {

    @Inject
    private Client client;

    @Inject
    private CookingConfig config;

    private boolean isWidgetOpen = false;

    @Provides
    CookingConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CookingConfig.class);
    }


    /*
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event){
        System.out.println(event.toString());
        if
        (
                !event.getMenuOption().contains("Cook") ||
                !event.getMenuTarget().contains("Range")
        )
        {
            return;
        }

        if(Widgets.get(270).isEmpty())
        {
            System.out.println("Empty");
        }
        else {
            System.out.println("not Empty");
        }
    }
     */

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() == 270) {
            this.isWidgetOpen = true;
        }
    }

    @Subscribe
    public void onClientTick(ClientTick e){
        if (isWidgetOpen){
            this.isWidgetOpen = false;
            Widget widget = Widgets.get(270)
                    .stream()
                    .filter(w -> w.getId() == 17694734)
                    .findFirst()
                    .get();
            widget.interact("Cook");
        }
    }


    private boolean hasFoodInInventory() {
        Item item = Inventory.getFirst(config.getFoods());
        return item != null;
    }

}