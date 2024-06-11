package OzonePlugins.modules;


import OzonePlugins.data.RaidState;

public interface PluginLifecycleComponent
{

	default boolean isEnabled(RaidState raidState)
	{
		return true;
	}

	void startUp();

	void shutDown();

}
