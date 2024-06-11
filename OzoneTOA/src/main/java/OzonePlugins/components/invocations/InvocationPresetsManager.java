package OzonePlugins.components.invocations;

import OzonePlugins.OzoneTOAConfig;
import OzonePlugins.modules.PluginLifecycleComponent;
import OzonePlugins.modules.RaidStateTracker;
import OzonePlugins.data.Invocation;
import OzonePlugins.data.RaidState;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class InvocationPresetsManager implements PluginLifecycleComponent
{

	public static final int WIDGET_ID_INVOCATIONS_PARENT = 774;
	public static final int WIDGET_ID_INVOCATIONS_SCROLLBAR = 51;
	public static final int WIDGET_ID_INVOCATIONS_CHILD = 52;
	public static final int SCRIPT_ID_BUILD_TOA_PARTY_INTERFACE = 6729;
	public static final int SCRIPT_ID_TOA_PARTY_TOGGLE_REWARD_PANEL = 6732;
	private static final String CONFIG_KEY_PRESETS = "presets";

	private final EventBus eventBus;
	private final ConfigManager configManager;

	private final Client client;
	private final OzoneTOAConfig config;
	private final ClientThread clientThread;
	private final ChatboxPanelManager chatboxPanelManager;
	private final RaidStateTracker raidStateTracker;

	@Getter
	private Set<Invocation> activeInvocations = EnumSet.noneOf(Invocation.class);

	@Getter
	private InvocationPreset currentPreset = null;

	private final SortedMap<String, InvocationPreset> presets = new TreeMap<>(Comparator.reverseOrder());
	private String originalHeaderText = null;

	@Override
	public boolean isEnabled(RaidState currentState)
	{
		return currentState.isInLobby();
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// This is run when the party screen is brought up, whenever a tab is changed, and whenever an invocation is clicked
		if (event.getScriptId() == SCRIPT_ID_BUILD_TOA_PARTY_INTERFACE || event.getScriptId() == SCRIPT_ID_TOA_PARTY_TOGGLE_REWARD_PANEL)
		{
			updateCurrentActiveInvocations();
			this.currentPreset = new InvocationPreset("current",activeInvocations);
			System.out.println(currentPreset.toStringDecorated());
		}
	}

	private void updateCurrentActiveInvocations()
	{
		Widget parent = client.getWidget(WIDGET_ID_INVOCATIONS_PARENT, WIDGET_ID_INVOCATIONS_CHILD);
		if (parent == null || parent.isHidden() || parent.getChildren() == null)
		{
			this.activeInvocations = EnumSet.noneOf(Invocation.class);
			return;
		}

		EnumSet<Invocation> activeCurrent = EnumSet.noneOf(Invocation.class);
		for (Invocation invoc : Invocation.values())
		{
			Widget invocW = parent.getChild(invoc.getWidgetIx());
			if (invocW == null)
			{
				continue;
			}

			Object[] ops = invocW.getOnOpListener();
			if (ops == null || ops.length < 4 || !(ops[3] instanceof Integer))
			{
				continue;
			}

			if ((Integer) ops[3] == 1)
			{
				activeCurrent.add(invoc);
			}
		}

		if (log.isDebugEnabled() && !activeCurrent.equals(activeInvocations))
		{
			Sets.SetView<Invocation> adds = Sets.difference(activeCurrent, activeInvocations);
			Sets.SetView<Invocation> removes = Sets.difference(activeInvocations, activeCurrent);
			log.debug("Invocations changed! Add: {}, Remove: {}", adds, removes);
		}
		this.activeInvocations = activeCurrent;
	}

}
