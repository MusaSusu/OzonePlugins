package ozone.zulrah.tasks;

import javax.inject.Singleton;

@Singleton
public class AttackZulrah extends OzoneTasks {

    @Override
    public void run()
    {
        plugin.attackZulrah();
    }

    @Override
    public String getName()
    {
        return "Attack Zulrah";
    }

}
