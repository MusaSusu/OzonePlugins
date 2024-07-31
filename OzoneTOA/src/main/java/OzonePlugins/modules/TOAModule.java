package OzonePlugins.modules;

import OzonePlugins.components.invocations.InvocationPresetsManager;
import OzonePlugins.components.kephri.ScabarasPuzzle.AdditionPuzzleSolver;
import OzonePlugins.components.kephri.ScabarasPuzzle.LayoutConfigurer;
import OzonePlugins.components.kephri.ScabarasPuzzle.ScabarasManager;
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
        lifecycleComponents.addBinding().to(ScabarasManager.class);
        lifecycleComponents.addBinding().to(AdditionPuzzleSolver.class);
        lifecycleComponents.addBinding().to(LayoutConfigurer.class);

    }
}
