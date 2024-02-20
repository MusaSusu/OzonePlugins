package ozone.cannon;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.TileObjects;
import org.pf4j.Extension;

import javax.inject.Inject;

@Extension
@PluginDescriptor(
        name = "Ozone cannon reloader",
        description = "Automically reloads cannon"
)
@Slf4j
public class OzoneCannon extends Plugin {

    @Inject
    private Client client;
    private boolean cannonPlaced;
    private boolean firstCannonLoad;
    private boolean isEmpty;
    private TileObject cannon;
    private WorldPoint clickedCannonLocation;
    private WorldPoint cannonPosition;
    private int tick;

    @Subscribe
    public void onGameTick(GameTick e)
    {
        if(tick > 0)
        {
            tick--;
            return;
        }
        if(isEmpty)
        {
            TileObjects.getNearest(cannonPosition,ObjectID.DWARF_MULTICANNON).interact("Fire");
            isEmpty = false;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();

        Player localPlayer = client.getLocalPlayer();
        if ((gameObject.getId() == ObjectID.CANNON_BASE || gameObject.getId() == ObjectID.CANNON_BASE_43029) && !cannonPlaced)
        {
            if (localPlayer.getWorldLocation().distanceTo(gameObject.getWorldLocation()) <= 2
                    && localPlayer.getAnimation() == AnimationID.BURYING_BONES)
            {
                cannonPosition = gameObject.getWorldLocation();
                clickedCannonLocation = gameObject.getWorldLocation();
                cannon = gameObject;
                System.out.println("game object spawned " +  gameObject.getName());
            }
        }
    }

    //needed for if player logs in with their cannon still there.
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (cannonPosition != null || (event.getId() != ObjectID.DWARF_MULTICANNON && event.getId() != ObjectID.DWARF_MULTICANNON_43027))
        {
            return;
        }

        // Check if cannonballs are being used on the cannon
        if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT
                && client.getSelectedWidget() != null
                && client.getSelectedWidget().getId() == ComponentID.INVENTORY_CONTAINER)
        {
            final Widget selected = client.getSelectedWidget();
            final int itemId = selected.getItemId();
            if (itemId != ItemID.CANNONBALL && itemId != ItemID.GRANITE_CANNONBALL)
            {
                return;
            }
        }
        // Check for the Fire option being selected on the cannon.
        else if (event.getMenuAction() != MenuAction.GAME_OBJECT_FIRST_OPTION)
        {
            return;
        }

        // Store the click location as a WorldPoint to avoid issues with scene loads
        clickedCannonLocation = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());
        log.debug("Updated cannon location: {}", clickedCannonLocation);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        if (event.getMessage().equals("You add the furnace."))
        {
            cannonPlaced = true;
            firstCannonLoad = true;
        }
        else if (event.getMessage().contains("You pick up the cannon")
                || event.getMessage().contains("Your cannon has decayed. Speak to Nulodion to get a new one!")
                || event.getMessage().contains("Your cannon has been destroyed!"))
        {
            cannonPlaced = false;
            cannonPosition = null;
        }
        else if (event.getMessage().startsWith("You load the cannon with"))
        {
            // Set the cannon's position and object if the player's animation was interrupted during setup
            if (cannonPosition == null && clickedCannonLocation != null)
            {

                System.out.println("Load cannon message");
                // There is a window of 1 tick where the player can add the furnace, click on another cannon, and then
                // the initial cannon load message arrives. This can cause the client to confuse the other cannon with
                // the player's, so ignore that first message when deciding the cannon's location.
                if (firstCannonLoad)
                {
                    firstCannonLoad = false;
                }
                else
                {
                    LocalPoint lp = LocalPoint.fromWorld(client, clickedCannonLocation);
                    if (lp != null)
                    {
                        GameObject[] objects = client.getScene().getTiles()[client.getPlane()][lp.getSceneX()][lp.getSceneY()].getGameObjects();
                        if (objects.length > 0 && client.getLocalPlayer().getWorldLocation().distanceTo(objects[0].getWorldLocation()) <= 2)
                        {
                            cannonPlaced = true;
                            cannon = objects[0];
                            cannonPosition = cannon.getWorldLocation();
                        }
                    }
                }
                clickedCannonLocation = null;
            }
        }
        else if (event.getMessage().contains("Your cannon is out of ammo!"))
        {
            System.out.println("cannon out of ammo message");
            this.isEmpty = true;
            tick = 4;
        }
        else if (event.getMessage().equals("This isn't your cannon!") || event.getMessage().equals("This is not your cannon."))
        {
            clickedCannonLocation = null;
        }
    }
}