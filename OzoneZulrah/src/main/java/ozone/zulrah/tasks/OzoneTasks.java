package ozone.zulrah.tasks;

import com.google.inject.Inject;
import net.runelite.api.Client;
import ozone.zulrah.OzoneZulrahPlugin;

public abstract class OzoneTasks {
    @Inject
    protected Client client;
    @Inject
    protected OzoneZulrahPlugin plugin;
    public abstract void run();
    public abstract String getName();
}
