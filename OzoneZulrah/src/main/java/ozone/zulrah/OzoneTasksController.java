package ozone.zulrah;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ExecutorService;
import OzonePlugins.OzoneTasks;
import lombok.Getter;


@Singleton
public class OzoneTasksController
{
    private ExecutorService executor;
    private List<OzoneTasks> tasks;
    private boolean isRunning = false;

    @Getter
    private String currentTask = "Example task";

    public boolean isRunning()
    {
        return isRunning;
    }
}
