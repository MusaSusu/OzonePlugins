package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.entities.Entities;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.Movement;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ScabarasManager implements PluginLifecycleComponent {

    private final Client client;
    private final EventBus eventBus;

    private ScabarasState scabarasState;
    private int gameTicks = 0;
    private int delayTicks;
    private boolean isFirstPuzzle;

    @Inject
    private LayoutConfigurer layoutConfigurer;
    @Inject
    private AdditionPuzzleSolver additionPuzzleSolver;
    @Inject
    private LightPuzzleSolver lightPuzzleSolver;
    @Inject
    private SequencePuzzleSolver sequencePuzzleSolver;
    @Inject
    private MatchingPuzzleSolver matchingPuzzleSolver;


    @Override
    public boolean isEnabled(RaidState raidState) {
        if (!raidState.isInRaid())
        {
            return false;
        }
        return raidState.getCurrentRoom() == RaidRoom.SCABARAS;
    }

    @Override
    public void startUp() {
        eventBus.register(this);
        reset();
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);
    }

    private void reset(){
        this.scabarasState = ScabarasState.START;
        isFirstPuzzle = true;
        delayTicks = 4;
    }

    public void run(RaidState raidState, boolean isPaused) {
        gameTicks++;
        if(isPaused)
        {
            return;
        }
        if(delayTicks > 0)
        {
            delayTicks--;
            return;
        }
        else {
            switch (scabarasState) {
                case START: {
                    LocalPoint flame = layoutConfigurer.getFlameLocation();
                    if (flame == null)
                    {
                        break;
                    }
                    else
                    {
                        LocalPoint destination = new LocalPoint(flame.getX() + 128, flame.getY());
                        if(client.getLocalPlayer().getLocalLocation().distanceTo(destination) == 0)
                        {
                            System.out.println("gameTick on arrive: " +  gameTicks);
                            this.scabarasState = layoutConfigurer.getCurrentPuzzle(isFirstPuzzle);
                            break;
                        }
                        if(!client.getLocalPlayer().isMoving())
                        {
                            TileObjects.getNearest(WorldPoint.fromLocal(client,flame), ObjectID.BARRIER_45135).interact("Quick-Pass");
                            System.out.println("gameTick on click: " +  gameTicks);
                            break;
                        }
                        break;
                    }
                }
                case ADDITION_PUZZLE:
                    additionPuzzleSolver.run();
                    break;
                case LIGHT_PUZZLE:
                    lightPuzzleSolver.run();
                    break;
                case SEQUENCE_PUZZLE:
                    sequencePuzzleSolver.run();
                    break;
                case MATCHING_PUZZLE:
                    matchingPuzzleSolver.run();
                    break;
                case END: {
                    break;
                }
            }
        }
    }

    private boolean checkTaskComplete(LocalPoint destination)
    {
        if (client.getLocalPlayer().getLocalLocation() != destination)
        {
            return false;
        }
        return true;
    }

    public void walkNextPuzzle()
    {
        if (isFirstPuzzle)
        {
            if(layoutConfigurer.atNextPuzzle(isFirstPuzzle))
            {
                this.isFirstPuzzle = false;
                this.scabarasState = layoutConfigurer.getCurrentPuzzle(isFirstPuzzle);
                return;
            }
            if(!client.getLocalPlayer().isMoving() && !client.getLocalPlayer().isAnimating())
            {
                TileObject passage = layoutConfigurer.getPassageLocation();
                if(passage == null)
                {
                    return;
                }
                layoutConfigurer.getPassageLocation().interact("Crawl-through");
            }
        }
        else
        {
            if (layoutConfigurer.atNextPuzzle(isFirstPuzzle))
            {
                this.scabarasState = ScabarasState.MATCHING_PUZZLE;
                return;
            }
            if(!client.getLocalPlayer().isMoving() && !client.getLocalPlayer().isAnimating())
            {
                layoutConfigurer.getPlatform().interact(0);
            }
            //walk to matching puzzle
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        if (e.getMessage().startsWith("Your party failed to complete the challenge"))
        {
            reset();
        }
    }

}
