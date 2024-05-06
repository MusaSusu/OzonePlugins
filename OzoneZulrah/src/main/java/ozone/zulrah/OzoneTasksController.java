package ozone.zulrah;

import javax.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ozone.zulrah.tasks.OzoneTasks;
import lombok.Getter;


@Singleton
public class OzoneTasksController
{
    private ExecutorService executor;
    private Queue<OzoneTasks> tasksQueue;
    private boolean isRunning = false;

    @Getter
    private String currentTask = "Example task";

    private static CompletableFuture<?> blockingTask;

    public OzoneTasksController()
    {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public void execBlocking(OzoneTasks ozoneTask)
    {
        isRunning = true;
        currentTask = ozoneTask.getName();
        blockingTask = CompletableFuture.runAsync(() -> {
            ozoneTask.run();
            isRunning = false;
            currentTask = "None";
        }, executor);
    }

    public void queueBlocking(OzoneTasks ozoneTask)
    {
        if(isRunning)
        {
            tasksQueue.add(ozoneTask);
        }
        else
        {
            execBlocking(ozoneTask);
        }
    }

    public void checkRunning()
    {
        if(!isRunning)
        {
            if (!tasksQueue.isEmpty())
            {
                execBlocking(tasksQueue.remove());
            }
        }
    }
}
