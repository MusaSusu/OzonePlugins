package ozone;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.Comparator;


@Extension
@PluginDescriptor(
        name = "[OZ] Prayer",
        description = "t-tick offer bones at altar",
        tags = {"Ozone","Skilling"}
)
@Slf4j
public class OzonePrayer extends Plugin {

    @Inject
    private Client client;

    private boolean tick;

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {

        if(event.getMenuTarget().contains("<col=ff9040>Bones<col=ffffff> -> "))
        {
            Item item = Inventory.getAll()
                    .stream()
                    .filter(x -> x.getName().toLowerCase().contains("bones"))
                    .max(Comparator.comparingInt(Item::getSlot))
                    .get();

            client.setSelectedSpellWidget(9764864);
            client.setSelectedSpellChildIndex(item.getSlot());
            client.setSelectedSpellItemId(item.getId());
            this.tick = true;
        }
        return;
    }


    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        createOfferMenu(event);
    }
    private void createOfferMenu(MenuEntryAdded event)
    {
        if (event.getType() != MenuAction.GAME_OBJECT_FIRST_OPTION.getId() ||
                event.isForceLeftClick() ||
                !event.getOption().toLowerCase().contains("pray") ||
                !event.getTarget().toLowerCase().contains("altar"))
        {
            return;
        }
        Item item = Inventory.getAll()
                .stream()
                .filter(x -> x.getName().toLowerCase().contains("bones"))
                .findFirst()
                .orElse(null);
        if(item == null)
        {
            return;
        }

        client.createMenuEntry(client.getMenuOptionCount())
                .setOption("Use")
                .setTarget("<col=ff9040>Bones<col=ffffff> -> " + event.getTarget())
                .setType(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
                .setIdentifier(event.getIdentifier())
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setForceLeftClick(true);
    }
}