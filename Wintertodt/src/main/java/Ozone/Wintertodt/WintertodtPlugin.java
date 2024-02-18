package Ozone.Wintertodt;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.wintertodt.config.WintertodtNotifyDamage;
import net.runelite.client.util.ColorUtil;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;

import javax.inject.Inject;

import java.time.Duration;
import java.time.Instant;

import static net.runelite.api.AnimationID.*;
import static net.runelite.api.AnimationID.CONSTRUCTION_IMCANDO;
import static net.runelite.client.plugins.wintertodt.config.WintertodtNotifyDamage.ALWAYS;
import static net.runelite.client.plugins.wintertodt.config.WintertodtNotifyDamage.INTERRUPT;


@Extension
@PluginDescriptor(
        name = " Wintertodt Assistant",
        description = "Helps with Wintertodt"
)
@Slf4j
public class WintertodtPlugin extends Plugin {

    private static final int WINTERTODT_REGION = 6462;
    @Inject
    private Client client;

    private WintertodtActivity currentActivity = WintertodtActivity.IDLE;
    private WintertodtActivity prevActivity = WintertodtActivity.IDLE;
    private boolean isInWintertodt;
    private Instant lastActionTime;

    private int tick = 2;

    private void reset()
    {
        //inventoryScore = 0;
        //totalPotentialinventoryScore = 0;
        //numLogs = 0;
        //numKindling = 0;
        currentActivity = WintertodtActivity.IDLE;
        lastActionTime = null;
    }

    private boolean isInWintertodtRegion()
    {
        if (client.getLocalPlayer() != null)
        {
            return client.getLocalPlayer().getWorldLocation().getRegionID() == WINTERTODT_REGION;
        }

        return false;
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event)
    {
        if(event.getMenuTarget().contains("<col=ff9040>Bruma herb</col><col=ffffff> -> "))
        {
            client.setSelectedSpellWidget(9764864);
            client.setSelectedSpellChildIndex(Inventory.getFirst("Bruma herb").getSlot());
            client.setSelectedSpellItemId(ItemID.BRUMA_HERB);
            return;
        }
        System.out.println(event.toString());
        System.out.println(client.getSelectedSpellWidget());
        System.out.println(client.getSelectedSpellChildIndex());
        System.out.println(client.getSelectedSpellChildIndex());
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event)
    {
        createPotionMenu(event);
    }

    private void createPotionMenu(MenuEntryAdded event) {
        if (!event.getOption().contains("Use") ||
                !event.getTarget().contains("Rejuvenation potion (unf)") ||
                event.isForceLeftClick() ||
                !Inventory.contains("Bruma herb")
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

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (!isInWintertodtRegion())
        {
            if (isInWintertodt)
            {
                log.debug("Left Wintertodt!");
                reset();
                isInWintertodt = false;
            }
            return;
        }

        if (!isInWintertodt)
        {
            reset();
            log.debug("Entered Wintertodt!");
            isInWintertodt = true;
        }

        checkActionTimeout();
    }

    private void checkActionTimeout()
    {
        if (tick > 0)
        {
            tick--;
            return;
        }
        if (prevActivity != null )
        {
            resumeActivity();
            return;
        }
        if (currentActivity == WintertodtActivity.IDLE)
        {
            return;
        }

        int currentAnimation = client.getLocalPlayer() != null ? client.getLocalPlayer().getAnimation() : -1;
        if (currentAnimation != IDLE || lastActionTime == null)
        {
            return;
        }

        Duration actionTimeout = Duration.ofSeconds(3);
        Duration sinceAction = Duration.between(lastActionTime, Instant.now());

        if (sinceAction.compareTo(actionTimeout) >= 0)
        {
            System.out.println("Activity timeout!");
            currentActivity = WintertodtActivity.IDLE;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        if (!isInWintertodt)
        {
            return;
        }

        ChatMessageType chatMessageType = chatMessage.getType();

        if (chatMessageType != ChatMessageType.GAMEMESSAGE && chatMessageType != ChatMessageType.SPAM)
        {
            return;
        }

        MessageNode messageNode = chatMessage.getMessageNode();
        final WintertodtInterruptType interruptType;

        if (messageNode.getValue().startsWith("You carefully fletch the root"))
        {
            setActivity(WintertodtActivity.FLETCHING);
            return;
        }

        if (messageNode.getValue().startsWith("The cold of"))
        {
            interruptType = WintertodtInterruptType.COLD;
        }
        else if (messageNode.getValue().startsWith("The freezing cold attack"))
        {
            interruptType = WintertodtInterruptType.SNOWFALL;
        }
        else if (messageNode.getValue().startsWith("The brazier is broken and shrapnel damages you"))
        {
            interruptType = WintertodtInterruptType.BRAZIER;
        }
        else if (messageNode.getValue().startsWith("You have run out of bruma roots"))
        {
            interruptType = WintertodtInterruptType.OUT_OF_ROOTS;
        }
        else if (messageNode.getValue().startsWith("Your inventory is too full"))
        {
            interruptType = WintertodtInterruptType.INVENTORY_FULL;
        }
        else if (messageNode.getValue().startsWith("You fix the brazier"))
        {
            interruptType = WintertodtInterruptType.FIXED_BRAZIER;
        }
        else if (messageNode.getValue().startsWith("You light the brazier"))
        {
            interruptType = WintertodtInterruptType.LIT_BRAZIER;
        }
        else if (messageNode.getValue().startsWith("The brazier has gone out."))
        {
            interruptType = WintertodtInterruptType.BRAZIER_WENT_OUT;
        }
        else
        {
            return;
        }

        boolean wasInterrupted = false;

        switch (interruptType)
        {
            case COLD:
                if (currentActivity != WintertodtActivity.WOODCUTTING && currentActivity != WintertodtActivity.IDLE)
                {
                    prevActivity  = currentActivity;
                    wasInterrupted = true;
                }
                break;
            case BRAZIER:
                if(currentActivity != WintertodtActivity.IDLE)
                {
                    prevActivity = currentActivity;
                    wasInterrupted = true;
                }
            case SNOWFALL:

                // Recolor message for damage notification
                client.refreshChat();

                // all actions except woodcutting and idle are interrupted from damage
                if (currentActivity != WintertodtActivity.WOODCUTTING && currentActivity != WintertodtActivity.IDLE)
                {
                    prevActivity  = currentActivity;
                    wasInterrupted = true;
                }

                break;
            case INVENTORY_FULL:
            case OUT_OF_ROOTS:
            case BRAZIER_WENT_OUT:
                wasInterrupted = true;
                if (currentActivity == WintertodtActivity.FEEDING_BRAZIER)
                {
                    prevActivity = WintertodtActivity.LIGHTING_BRAZIER;
                }
                break;
            case LIT_BRAZIER:
                prevActivity = WintertodtActivity.FEEDING_BRAZIER;
            case FIXED_BRAZIER:
                prevActivity = WintertodtActivity.LIGHTING_BRAZIER;
                wasInterrupted = true;
                break;
        }
        if (wasInterrupted)
        {
            if (prevActivity == WintertodtActivity.FLETCHING)
            {
                this.tick = 2;
            }
            currentActivity = WintertodtActivity.IDLE;
        }
    }

    @Subscribe
    public void onAnimationChanged(final AnimationChanged event)
    {
        if (!isInWintertodt)
        {
            return;
        }

        final Player local = client.getLocalPlayer();

        if (event.getActor() != local)
        {
            return;
        }

        final int animId = local.getAnimation();
        switch (animId)
        {
            case WOODCUTTING_BRONZE:
            case WOODCUTTING_IRON:
            case WOODCUTTING_STEEL:
            case WOODCUTTING_BLACK:
            case WOODCUTTING_MITHRIL:
            case WOODCUTTING_ADAMANT:
            case WOODCUTTING_RUNE:
            case WOODCUTTING_GILDED:
            case WOODCUTTING_DRAGON:
            case WOODCUTTING_DRAGON_OR:
            case WOODCUTTING_INFERNAL:
            case WOODCUTTING_3A_AXE:
            case WOODCUTTING_CRYSTAL:
            case WOODCUTTING_TRAILBLAZER:
            case WOODCUTTING_2H_BRONZE:
            case WOODCUTTING_2H_IRON:
            case WOODCUTTING_2H_STEEL:
            case WOODCUTTING_2H_BLACK:
            case WOODCUTTING_2H_MITHRIL:
            case WOODCUTTING_2H_ADAMANT:
            case WOODCUTTING_2H_RUNE:
            case WOODCUTTING_2H_DRAGON:
            case WOODCUTTING_2H_CRYSTAL:
            case WOODCUTTING_2H_CRYSTAL_INACTIVE:
            case WOODCUTTING_2H_3A:
                setActivity(WintertodtActivity.WOODCUTTING);
                break;

            case FLETCHING_BOW_CUTTING:
                setActivity(WintertodtActivity.FLETCHING);
                break;

            case LOOKING_INTO:
                setActivity(WintertodtActivity.FEEDING_BRAZIER);
                break;

            case FIREMAKING:
                setActivity(WintertodtActivity.LIGHTING_BRAZIER);
                break;

            case CONSTRUCTION:
            case CONSTRUCTION_IMCANDO:
                setActivity(WintertodtActivity.FIXING_BRAZIER);
                break;
        }
    }

    private void setActivity(WintertodtActivity action)
    {
        currentActivity = action;
        lastActionTime = Instant.now();
    }

    private void resumeActivity()
    {
        switch (prevActivity)
        {
            case FLETCHING:
                System.out.println("Prev acitvity = fletching");
                Inventory.getFirst("Knife").useOn(Inventory.getFirst(ItemID.BRUMA_ROOT));
                break;
            case FEEDING_BRAZIER:
                System.out.println("Prev acitvity = feed brazier");
                TileObjects.getNearest("Burning brazier","Brazier").interact(0);
                break;
            case FIXING_BRAZIER:
                System.out.println("Prev activity = fixing brazier");
                TileObjects.getNearest("Burning brazier","Brazier").interact(0);
                break;
            case LIGHTING_BRAZIER:
                System.out.println("Prev acitvity = lighting brazier");
                TileObjects.getNearest("Burning brazier","Brazier").interact(0);
                break;
            default:
                break;
        }
        prevActivity = null;
    }

}