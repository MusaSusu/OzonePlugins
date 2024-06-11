package OzonePlugins.modules;

import OzonePlugins.components.invocations.InvocationPresetsManager;
import OzonePlugins.components.zebak.ZebakManager;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class TOAModule extends AbstractModule {

    @Override
    protected void configure()
    {
        Multibinder<PluginLifecycleComponent> lifecycleComponents = Multibinder.newSetBinder(binder(), PluginLifecycleComponent.class);
        lifecycleComponents.addBinding().to(RaidStateTracker.class);
        lifecycleComponents.addBinding().to(InvocationPresetsManager.class);
        lifecycleComponents.addBinding().to(ZebakManager.class);
    }
}
