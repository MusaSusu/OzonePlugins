package OzonePlugins.components.kephri.ScabarasPuzzle;

import OzonePlugins.data.RaidRoom;
import OzonePlugins.data.RaidState;
import OzonePlugins.modules.PluginLifecycleComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.unethicalite.api.entities.TileObjects;

import javax.inject.Inject;
import javax.inject.Singleton;
/*
Current antibots. Automatically sets to true all the matching tiles by spawning the gameobjects.
Platform and tunnel loc is glitched.
Possible glitch in sequence tiles
 */

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ScabarasManager implements PluginLifecycleComponent {

    private final Client client;
    private final EventBus eventBus;

    @Getter
    @Setter
    private ScabarasState scabarasState;
    private int gameTicks = 0;
    private int delayTicks;

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
                            this.scabarasState = layoutConfigurer.getNextPuzzle(scabarasState);
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
                    if(!client.getLocalPlayer().isMoving())
                    {
                        TileObject entry = TileObjects.getFirstSurrounding(WorldPoint.fromScene(client.getLocalPlayer().getWorldView(),78,48,0),3, ObjectID.ENTRY_45337);
                        if(entry == null)
                        {
                            System.out.println("Entry null");
                            return;
                        }
                        entry.interact("Quick-Enter");
                    }
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
        if(layoutConfigurer.atNextPuzzle(scabarasState))
        {
            this.scabarasState = layoutConfigurer.isFirstPuzzle(scabarasState) ? layoutConfigurer.getNextPuzzle(scabarasState) : ScabarasState.MATCHING_PUZZLE;
            return;
        }
        if(!client.getLocalPlayer().isMoving() && !client.getLocalPlayer().isAnimating())
        {
            TileObject passage = layoutConfigurer.getObstacle(scabarasState);
            if(passage == null)
            {
                System.out.println("Passage null");
                return;
            }
            passage.interact(0);
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
