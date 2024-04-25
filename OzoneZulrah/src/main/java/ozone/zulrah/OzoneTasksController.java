package ozone.zulrah;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ExecutorService;


@Singleton
public class OzoneTasksController
{
    private ExecutorService executor;

    private List<Runnable> tasks;
}
