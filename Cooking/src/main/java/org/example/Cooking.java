package org.example;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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
import java.sql.Time;
import java.time.LocalTime;
import java.util.Comparator;


@Extension
@PluginDescriptor(
        name = " Ozone Cooking Assist",
        description = "Helps with cooking"
)
@Slf4j
public class Cooking extends Plugin {

    @Inject
    private Client client;

    @Inject
    private CookingConfig config;

    private boolean isWidgetOpen = false;
    private boolean hasKarambwan = false;
    private boolean isWine = false;
    private boolean tick = false;

    @Provides
    CookingConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CookingConfig.class);
    }


    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event){
        if (tick)
        {
            event.consume();
            return;
        }

        if (event.getMenuTarget().contains("<col=ff9040>Raw karambwan<col=ffffff> -> "))
        {
            client.setSelectedSpellWidget(9764864);
            client.setSelectedSpellChildIndex(
                    Inventory.getAll("Raw karambwan")
                            .stream()
                            .max(Comparator.comparingInt(Item::getSlot))
                            .orElse(null)
                            .getSlot());
            client.setSelectedSpellItemId(ItemID.RAW_KARAMBWAN);
            this.tick = true;
            return;
        }

        if (event.getMenuTarget().contains("<col=ff9040>Jug of water</col><col=ffffff> -> "))
        {
            client.setSelectedSpellWidget(9764864);
            client.setSelectedSpellChildIndex(Inventory.getFirst("Jug of water").getSlot());
            client.setSelectedSpellItemId(ItemID.JUG_OF_WATER);
            this.isWine = true;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        createKarambwanMenu(event);
        createWineMenu(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() == 270) {
            this.isWidgetOpen = true;
            System.out.println("widget loaded:" + Time.valueOf(LocalTime.now()));
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (tick)
        {
            this.tick = false;
        }
        if(isWidgetOpen && hasKarambwan)
        {
            this.isWidgetOpen = false;
            Widgets.get(270)
                    .stream()
                    .filter(w -> w.getId() == 17694735)
                    .findFirst()
                    .ifPresent( w -> w.interact("Cook"));
            return;
        }
        if(isWidgetOpen)
        {
            Widget widget = Widgets.get(270)
                    .stream()
                    .filter(w-> w.getId() == 17694725)
                    .findFirst()
                    .orElse(null);
            if (widget == null || !(widget.getText().contains("cook") || this.isWine ) )
            {
                return;
            }
            Widgets.get(270)
                    .stream()
                    .filter(w-> w.getId() == 17694732)
                    .findFirst()
                    .ifPresent(w -> w.interact("All"));
            Widgets.get(270)
                    .stream()
                    .filter(w -> w.getId() == 17694734)
                    .findFirst()
                    .ifPresent( w -> w.interact(0));
            this.isWine = false;
        }
    }
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.INVENTORY.getId() || event.getItemContainer() == null)
        {
            return;
        }
        this.hasKarambwan = Inventory.contains("Raw karambwan");
    }

    private void createKarambwanMenu(MenuEntryAdded event)
    {
        if(!hasKarambwan || event.isForceLeftClick())
        {
            return;
        }
        if (!event.getOption().contains("Cook") && !event.getTarget().contains("Fire") )
        {
            return;
        }

        client.createMenuEntry(client.getMenuOptionCount())
                .setOption("Use")
                .setTarget("<col=ff9040>Raw karambwan<col=ffffff> -> " + event.getTarget())
                .setType(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
                .setIdentifier(event.getIdentifier())
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setForceLeftClick(true);
    }

    private void createWineMenu(MenuEntryAdded event) {
        if (!event.getOption().contains("Use") ||
                !event.getTarget().contains("Grapes") ||
                event.isForceLeftClick() ||
                !Inventory.contains("Jug of water")
        )
        {
            return;
        }
        client.createMenuEntry(client.getMenuOptionCount())
                .setOption("Use")
                .setTarget("<col=ff9040>Jug of water</col><col=ffffff> -> " + event.getTarget())
                .setType(MenuAction.WIDGET_TARGET_ON_WIDGET)
                .setIdentifier(0)
                .setParam0(event.getActionParam0())
                .setParam1(9764864)
                .setForceLeftClick(true);
    }


    private boolean hasFoodInInventory() {
        Item item = Inventory.getFirst(config.getFoods());
        return item != null;
    }

}