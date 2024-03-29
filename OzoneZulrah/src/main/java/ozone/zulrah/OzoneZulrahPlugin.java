package ozone.zulrah;

import com.google.common.base.Preconditions;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.input.naturalmouse.NaturalMouse;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Tab;
import net.unethicalite.api.widgets.Tabs;
import org.pf4j.Extension;
import ozone.zulrah.data.GearSetup;
import ozone.zulrah.data.StandLocation;
import ozone.zulrah.data.ZulrahType;
import ozone.zulrah.rotationutils.RotationType;
import ozone.zulrah.rotationutils.ZulrahData;
import ozone.zulrah.rotationutils.ZulrahPhase;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


@Extension
@PluginDescriptor(
        name = "Ozone Zulrah",
        enabledByDefault = false
)
@Slf4j
public class OzoneZulrahPlugin extends Plugin {
    @Inject
    private NaturalMouse naturalMouse;
    @Inject
    private Client client;
    @Inject
    private OzoneZulrahConfig config;
    @Inject
    private KeyManager keyManager;
    private ExecutorService executor;
    private NPC zulrahNpc = null;
    private int stage = 0;
    private int phaseTicks = -1;
    private int attackTicks = -1;
    private int totalTicks = 0;
    private RotationType currentRotation = null;
    private List<RotationType> potentialRotations = new ArrayList<RotationType>();
    private final Map<LocalPoint, Integer> projectilesMap = new HashMap<LocalPoint, Integer>();
    private final Map<GameObject, Integer> toxicCloudsMap = new HashMap<GameObject, Integer>();
    @Getter
    private static boolean flipStandLocation = false;
    @Getter
    private static boolean flipPhasePrayer = false;
    @Getter
    private static boolean zulrahReset = false;
    private final Collection<NPC> snakelings = new ArrayList<NPC>();
    private int movementTicks = 0;
    private int playerAttackTicks = 0;
    private boolean shouldAttack;
    private boolean shouldPray;
    private boolean shouldChangeGear;
    private boolean shouldPickItemsUp;
    private int attacksLeft = 0;
    private LocalPoint dest;
    private LocalPoint nextDest;
    private static volatile boolean isBlocking = false;
    private static CompletableFuture<?> blockingTask;
    private ZulrahPhase refZulrah;
    private Collection<ItemStack> loot;

    private final BiConsumer<RotationType, RotationType> phaseTicksHandler = (current, potential) -> {
        if (zulrahReset)
        {
            phaseTicks = 38;
        }
        else
        {
            ZulrahPhase p = getCurrentPhase();
            Preconditions.checkNotNull(p, "Attempted to set phase ticks but current Zulrah phase was somehow null. Stage: " + stage);
            phaseTicks = p.getAttributes().getPhaseTicks();
        }
    };


    @Provides
    OzoneZulrahConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OzoneZulrahConfig.class);
    }

    @Override
    protected void startUp()
    {

        List<String> rangeGearNames = Arrays.stream(config.rangeGearNames().split(","))
                .collect(Collectors.toList());
        List<String> mageGearNames = Arrays.stream(config.mageGearNames().split(","))
                .collect(Collectors.toList());
        ZulrahType.setMagePhaseGear(new GearSetup(rangeGearNames));
        ZulrahType.setRangedMeleePhaseGear(new GearSetup(mageGearNames));
        executor = Executors.newSingleThreadExecutor();

/*
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            keyManager.registerKeyListener(gearSwitcher);
        }

 */
    }
    @Override
    public void shutDown()
    {
        System.out.println("SHUTDOWN");
        reset();
        executor.shutdownNow();
        keyManager.unregisterKeyListener(gearSwitcher);
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged event)
    {
        if (!(event.getActor() instanceof NPC))
        {
            return;
        }
        NPC npc = (NPC)((Object)event.getActor());
        if (npc.getName() != null && !npc.getName().equalsIgnoreCase("zulrah"))
        {
            return;
        }
        System.out.println(npc.getAnimation());
        switch (npc.getAnimation())
        {
            case 5071:
            {
                //start of phase
                zulrahNpc = npc;
                potentialRotations = RotationType.findPotentialRotations(npc, stage);
                phaseTicksHandler.accept(currentRotation, potentialRotations.get(0));
                log.info("New Zulrah Encounter Started");

                dest = StandLocation.NORTHEAST_TOP.toLocalPoint();
                this.attacksLeft = 9;
                shouldAttack = true;
                this.refZulrah = getCurrentPhase();
                execBlocking(this::changePrays);
                execBlocking(this::prePray);
                break;
            }
            case 5073:
            {
                System.out.println("Zulrah coming up");
                //zulrah coming up
                if (zulrahReset)
                {
                    zulrahReset = false;
                    zulrahNpc = npc;
                }
                else
                {
                    ++stage;
                }

                if (currentRotation == null)
                {
                    potentialRotations = RotationType.findPotentialRotations(npc, stage);
                    currentRotation = potentialRotations.size() == 1 ? potentialRotations.get(0) : null;
                }
                //phaseTicksHandler.accept(currentRotation, potentialRotations.get(0));
                System.out.println(currentRotation);

                if(stage == 1)
                {
                    shouldChangeGear = true;
                    shouldPray = true;
                    this.refZulrah = getCurrentPhase();
                }

                setAttacksLeft();
                shouldAttack = true;

                if(currentRotation != null)
                {
                    if (isLastPhase(currentRotation)){
                        System.out.println("last phase should attack bool: " + shouldAttack );
                        break;
                    }
                }
                setNextDest();

                break;
            }
            case 5072:
            {
                System.out.println("Zulrah going down");
                //zulrah going down

                shouldAttack = false;

                if(getCurrentPhase().getZulrahNpc().getType() == ZulrahType.MELEE)
                {
                    setDest();
                }

                if(stage > 0)
                {
                    shouldChangeGear = true;
                    shouldPray = true;
                    this.refZulrah = currentRotation == null ? getNextPhase(potentialRotations.get(0)) : getNextPhase(currentRotation);
                }

                if (currentRotation == null || !isLastPhase(currentRotation)) break;
                stage = 0;
                currentRotation = null;
                potentialRotations.clear();
                snakelings.clear();
                flipStandLocation = false;
                flipPhasePrayer = false;
                zulrahReset = true;
                System.out.println("Resetting Zulrah");
                break;
            }
            case 5069: {
                System.out.println("Attack animation");
                System.out.println("Attacks left:" + attacksLeft);
                //normal attacks
                attackTicks = 4;
                attacksLeft--;
                if(attacksLeft == 0)
                {
                    setDest();
                    checkShouldAttack();
                }
                if (currentRotation == null || !getCurrentPhase().getZulrahNpc().isJad()) break;
                if(attacksLeft < 0)
                {
                    break;
                }
                flipPhasePrayer = !flipPhasePrayer;
                execBlocking(this::changeJadPrays);
                break;
            }
            case 5806:
            case 5807:
            {
                System.out.println("Melee jad attack");
                System.out.println("Attacks left:" + attacksLeft);

                //melee jad attack
                attacksLeft--;
                attackTicks = 8;
                flipStandLocation = !flipStandLocation;
                getCurrentPhase().getAttributes().getCurrentDynamicStandLocation().ifPresent(x-> dest = x.toLocalPoint());
                break;
            }
            case 5808:
                System.out.println("Turning to face");
                break;
            case 5804:
            {
                //zuralh death anim
                reset();
            }
        }
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (shouldPickItemsUp && loot != null)
        {
            pickItemsUp();
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN || zulrahNpc == null) {
            return;
        }
        ++totalTicks;
        if (attackTicks > 0) {
            --attackTicks;
        }
        if (phaseTicks >= 0) {
            --phaseTicks;
        }
        if (!projectilesMap.isEmpty()) {
            projectilesMap.values().removeIf(v -> v <= 0);
            projectilesMap.replaceAll((k, v) -> v - 1);
        }
        if (!toxicCloudsMap.isEmpty()) {
            toxicCloudsMap.values().removeIf(v -> v <= 0);
            toxicCloudsMap.replaceAll((k, v) -> v - 1);
        }
        if(playerAttackTicks >0)
        {
            playerAttackTicks--;
        }
        if (isBlocking) {
            return;
        }
        if (dest != null) {
            if (movementTicks > 0)
            {
                movementTicks--;
            } else {
                moveToTile();
                return;
            }
        }
        if(shouldPray)
        {
            execBlocking(this::changePrays);
            return;
        }
        if (shouldChangeGear) {
            if(dest != null && !Players.getLocal().isMoving()) //check for this so we only change gear while moving to save ticks.
            {
                return;
            }
            execBlocking(this::switchGear);
            return;
        }
        if (checkHealth()) {
            return;
        }
        if(checkPrayer())
        {
            return;
        }
        if(checkPoison())
        {
            return;
        }
        if (shouldAttack) {
            if (playerAttackTicks > 0) {
                return;
            } else {
                attackZulrah();
            }
        }
    }

    @Nullable
    private ZulrahPhase getCurrentPhase()
    {
        RotationType type = currentRotation;
        if(currentRotation == null)
        {
            type = potentialRotations.get(0);
        }
        return stage >= type.getZulrahPhases().size() ? null : type.getZulrahPhases().get(stage);
    }


    @Nullable
    private ZulrahPhase getNextPhase(RotationType type)
    {
        return isLastPhase(type) ? null : type.getZulrahPhases().get(stage + 1);
    }

    private boolean isLastPhase(RotationType type)
    {
        return stage == type.getZulrahPhases().size() - 1;
    }

    public Set<ZulrahData> getZulrahData()
    {
        LinkedHashSet<ZulrahData> zulrahDataSet = new LinkedHashSet<ZulrahData>();
        if (currentRotation == null)
        {
            potentialRotations.forEach(type -> zulrahDataSet.add(new ZulrahData(getCurrentPhase(), getNextPhase((RotationType)((Object)type)))));
        }
        else
        {
            zulrahDataSet.add(new ZulrahData(getCurrentPhase(), getNextPhase(currentRotation)));
        }
        return !zulrahDataSet.isEmpty() ? zulrahDataSet : Collections.emptySet();
    }

    private void reset()
    {
        zulrahNpc = null;
        stage = 0;
        phaseTicks = -1;
        attackTicks = -1;
        totalTicks = 0;
        currentRotation = null;
        potentialRotations.clear();
        projectilesMap.clear();
        toxicCloudsMap.clear();
        flipStandLocation = false;
        flipPhasePrayer = false;
        zulrahReset = false;
        log.info("Zulrah Reset!");
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        log.info("GameStateChanged");
        if (zulrahNpc == null)
        {
            return;
        }
        switch (event.getGameState())
        {
            case LOADING:
            case CONNECTION_LOST:
            case HOPPING:
            {
                reset();
            }
        }
    }

    private boolean checkHealth()
    {
        if (client.getBoostedSkillLevel(Skill.HITPOINTS) < 50)
        {
            Item food1 = Inventory.getFirst(x-> x.getId() == ItemID.SHARK);
            Item food2 = Inventory.getFirst(x-> x.getId() == ItemID.COOKED_KARAMBWAN);
            if (food1 != null & food2 != null )
            {
                execBlocking(()-> {
                    if(!Tabs.isOpen(Tab.INVENTORY))
                    {
                        Tabs.open(Tab.INVENTORY);
                        Time.sleep(5,20);
                    }
                    food1.interact("Eat");
                    Time.sleep(20,30);
                    food2.interact("Eat");
                });
                return true;
            }
            else if (food1 != null)
            {
                food1.interact("Eat");
                return true;
            }
            else
            {
                //should tp out
                return false;
            }
        }
        return false;
    }

    private boolean checkPrayer()
    {
        if (client.getBoostedSkillLevel(Skill.PRAYER) < 20)
        {
            Item pot = Inventory.getFirst(x-> x.getName().contains("Prayer potion"));
            if (pot != null)
            {
                pot.interact("Drink");
                return true;
            }
            else
            {
                //should tp out
                return false;
            }
        }
        return false;
    }

    private boolean checkPoison()
    {
        if (Combat.isPoisoned())
        {
            Item pot = Inventory.getFirst(x-> x.getName().contains("Anti-venom"));
            if(pot != null)
            {
                pot.interact("Drink");
                return true;
            }
            else {
                //should tp out
                return false;
            }
        }
        return false;
    }
    private void attackZulrah()
    {
        if(!Players.getLocal().isInteracting())
        {
            zulrahNpc.interact("Attack");
            attackTicks = 5;
            movementTicks = 1;
        }
    }

    private void moveToTile()
    {

        WorldPoint target = WorldPoint.fromLocal(client,dest);
        if (Players.getLocal().getWorldLocation().distanceTo(target)>0)
        {
            if(!Movement.isWalking())
            {
                Movement.walk(target);
                movementTicks = 4;
            }
        }
        else {
            dest = null;
        }
    }

    private void changePrays()
    {
        Prayer prayer = refZulrah.getAttributes().getPrayer();
        Prayer damagePrayer = refZulrah.getZulrahNpc().getType() == ZulrahType.MAGIC ? config.rangePrayer().getPrayer() : config.magePrayer().getPrayer();

        if(prayer == null)
        {
        }
        else if(!Prayers.isEnabled(prayer))
        {
            if(!Tabs.isOpen(Tab.PRAYER))
            {
                Tabs.open(Tab.PRAYER);
            }
            Prayers.toggle(prayer);
        }
        if(!Prayers.isEnabled(damagePrayer))
        {
            if(!Tabs.isOpen(Tab.PRAYER))
            {
                Tabs.open(Tab.PRAYER);
            }
            Prayers.toggle(damagePrayer);
        }
        shouldPray = false;
    }

    private void changeJadPrays()
    {
        Prayer prayer = getCurrentPhase().getAttributes().getPrayer();
        if (flipPhasePrayer)
        {
            if(!Tabs.isOpen(Tab.PRAYER))
            {
                Tabs.open(Tab.PRAYER);
            }
            if (prayer == Prayer.PROTECT_FROM_MAGIC)
            {
                if(!Prayers.isEnabled(Prayer.PROTECT_FROM_MISSILES))
                {
                    Prayers.toggle(Prayer.PROTECT_FROM_MISSILES);
                }
            }
            else
            {
                if(!Prayers.isEnabled(Prayer.PROTECT_FROM_MAGIC))
                {
                    Prayers.toggle(Prayer.PROTECT_FROM_MAGIC);
                }
            }
        }
        else
        {
            if(!Tabs.isOpen(Tab.PRAYER))
            {
                Tabs.open(Tab.PRAYER);
            }
            assert prayer != null;
            if(!Prayers.isEnabled(prayer))
            {
                Prayers.toggle(prayer);
            }
        }
    }
    private void switchGear() {
        if (!Tabs.isOpen(Tab.INVENTORY))
        {
            Tabs.open(Tab.INVENTORY);
        }
        if (refZulrah.getZulrahNpc().getType() == ZulrahType.MAGIC && !refZulrah.getZulrahNpc().isJad())
        {
            ZulrahType.MAGIC.getSetup().switchGear(10);
            rangePot();
        }
        else
        {
            ZulrahType.RANGE.getSetup().switchGear(10);
        }
        shouldChangeGear = false;
    }
    private void execBlocking(Runnable r)
    {
        if(isBlocking)
        {
            blockingTask.thenRunAsync(()-> {
                isBlocking = true;
                r.run();
                isBlocking = false;
                },executor);
            return;
        }

        isBlocking = true;

        blockingTask = CompletableFuture.runAsync(() -> {
            r.run();
            isBlocking = false;
        }, executor);
    }

    private void setDest()
    {
        System.out.println("Setting tile to walk");
        this.dest = this.nextDest;
        this.movementTicks = getCurrentPhase().getAttributes().getTicksToMove();
    }

    private void setNextDest()
    {
        ZulrahPhase p = currentRotation == null ? getNextPhase(potentialRotations.get(0)) : getNextPhase(currentRotation);
        nextDest = p.getAttributes().getStandLocation().toLocalPoint();
    }
    private void checkShouldAttack()
    {
        shouldAttack = getCurrentPhase().getAttributes().isShouldAttack();
    }

    private void setAttacksLeft()
    {
        if (zulrahReset)
        {
            attacksLeft = 9;
        }
        else
        {
            ZulrahPhase p = getCurrentPhase();
            Preconditions.checkNotNull(p, "Attempted to set phase ticks but current Zulrah phase was somehow null. Stage: " + stage);
            attacksLeft = p.getAttributes().getPhaseAttacks();
            if(currentRotation != null)
            {
                System.out.println("rotation = B check" + currentRotation.getRotationName().equals("Rotation B"));
                System.out.println("equipment check" + p.getZulrahNpc().getType().getSetup().hasExactItem("Toxic blowpipe"));
            }
            if (stage == 4 && currentRotation.getRotationName().equals("Rotation B") && p.getZulrahNpc().getType().getSetup().hasExactItem("Toxic blowpipe"))
            {
                System.out.println("rotation B change attacks left");
                attacksLeft = 5;
            }
        }
    }

    private void pickItemsUp()
    {
        executor.execute(()-> {
            LocalPoint itemLoc = loot.iterator().next().getLocation();
            Point canvas = Perspective.localToCanvas(client,itemLoc,0);
            naturalMouse.moveTo(canvas.getX(), canvas.getY());
            loot = null;
            shouldPickItemsUp = false;
                }
        );
    }

    private void rangePot()
    {
        if(Skills.getBoostedLevel(Skill.RANGED) - Skills.getLevel(Skill.RANGED) >= 5)
        {
            return;
        }
        Item rangePot = Inventory.getFirst(x-> x.getName().contains("Ranging potion"));
        if (rangePot != null)
        {
            execBlocking( () -> {
                rangePot.interact("Drink");
                    }
            );
        }
    }

    private void prePray()
    {
        Prayer prayer = Prayer.PRESERVE;
        if(!Tabs.isOpen(Tab.PRAYER))
        {
            Tabs.open(Tab.PRAYER);
        }
        if(!Prayers.isEnabled(prayer))
        {
            Prayers.toggle(prayer);
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived npcLootReceived)
    {
        if (npcLootReceived.getNpc().getName().equals("Zulrah"))
        {
            shouldPickItemsUp = true;
            loot = npcLootReceived.getItems();
        }
    }

    private final HotkeyListener gearSwitcher = new HotkeyListener(() -> config.getSpecHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            //switchGear();
        }
    };

}