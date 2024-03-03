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
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.widgets.Prayers;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private Client client;

    @Inject
    private OzoneZulrahConfig config;
    @Inject
    private KeyManager keyManager;
    private ScheduledExecutorService executor;
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

    private int attacksLeft = 0;
    private LocalPoint dest;
    private LocalPoint nextDest;


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
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void shutDown()
    {
        System.out.println("SHUTDOWN");
        reset();
        executor.shutdownNow();
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
        // 5808 = facing?
        //
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
                break;
            }
            case 5073:
            {
                System.out.println("Zulrah coming up");
                //zulrah coming up
                ++stage;
                if (currentRotation == null)
                {
                    potentialRotations = RotationType.findPotentialRotations(npc, stage);
                    currentRotation = potentialRotations.size() == 1 ? potentialRotations.get(0) : null;
                }
                //phaseTicksHandler.accept(currentRotation, potentialRotations.get(0));
                setAttacksLeft();
                setNextDest();
                System.out.println(currentRotation);
                shouldAttack = true;
                shouldChangeGear = true;
                changePrays();
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
                if (zulrahReset)
                {
                    zulrahReset = false;
                }
                if (currentRotation == null || !isLastPhase(currentRotation)) break;
                stage = -1;
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
                }
                if (currentRotation == null || !getCurrentPhase().getZulrahNpc().isJad()) break;
                flipPhasePrayer = !flipPhasePrayer;
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
                if(currentRotation == null)
                {
                    getCurrentPhase().getAttributes().getCurrentDynamicStandLocation().ifPresent(x-> dest = x.toLocalPoint());
                }
                else
                {
                    getCurrentPhase().getAttributes().getCurrentDynamicStandLocation().ifPresent(x-> dest = x.toLocalPoint());
                }
                break;
            }
            case 5808:
                System.out.println("Turning to face");
                break;
            case 5804:
            {
                reset();
            }
        }
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || zulrahNpc == null)
        {
            return;
        }
        ++totalTicks;
        if (attackTicks >= 0)
        {
            --attackTicks;
        }
        if (phaseTicks >= 0)
        {
            --phaseTicks;
        }
        if (!projectilesMap.isEmpty())
        {
            projectilesMap.values().removeIf(v -> v <= 0);
            projectilesMap.replaceAll((k, v) -> v - 1);
        }
        if (!toxicCloudsMap.isEmpty())
        {
            toxicCloudsMap.values().removeIf(v -> v <= 0);
            toxicCloudsMap.replaceAll((k, v) -> v - 1);
        }
        if (dest != null)
        {
            if(movementTicks > 0)
            {
                movementTicks--;
            }
            else {
                moveToTile();
                return;
            }
        }
        /*
        if(shouldChangeGear)
        {
            switchGear();
            return;
        }
         */
        /*
        if(shouldAttack)
        {
            if(attackTicks > 0)
            {
                attackTicks--;
            }
            else
            {
                attackZulrah();
            }
        }
         */
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
        return zulrahDataSet.size() > 0 ? zulrahDataSet : Collections.emptySet();
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

    private void attackZulrah()
    {
        if(!Players.getLocal().isInteracting())
        {
            zulrahNpc.interact("Attack");
            attackTicks = 3;
            movementTicks=1;
        }
    }

    private void moveToTile()
    {

        WorldPoint target = WorldPoint.fromLocal(client,dest);
        if (Players.getLocal().getWorldLocation().distanceTo(target)>0)
        {
            Movement.walk(target);
            movementTicks = 4;
        }
        else {
            dest = null;
        }
    }

    private void changePrays()
    {
        Prayer prayer = getCurrentPhase().getAttributes().getPrayer();
        if(prayer == null)
        {
            return;
        }
        if(!Prayers.isEnabled(prayer))
        {
            Prayers.toggle(prayer);
        }
        return;
    }
    private void switchGear()
    {
        ZulrahType state = getCurrentPhase().getZulrahNpc().getType();
        if (state == ZulrahType.MAGIC)
        {
            executor.execute(()->ZulrahType.MAGIC.getSetup().switchGear(50));
            shouldChangeGear = false;
        }
        else
        {
           executor.execute(()->ZulrahType.RANGE.getSetup().switchGear(50));
           shouldChangeGear = false;
        }
    }

    private void setDest()
    {
        System.out.println("Setting tile to walk");
        this.dest = this.nextDest;
        this.movementTicks = getCurrentPhase().getAttributes().getTicksToMove();
    }

    private void setNextDest()
    {
        ZulrahPhase p;
        if (currentRotation != null)
        {
             p = getNextPhase(currentRotation);
        }
        else
        {
            p = getNextPhase(potentialRotations.get(0));
        }
        nextDest = p.getAttributes().getStandLocation().toLocalPoint();
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
        }
    }
    private final HotkeyListener gearSwitcher = new HotkeyListener(() -> config.getSpecHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            switchGear();
            System.out.println("Hotkeypressed");
        }
    };

}