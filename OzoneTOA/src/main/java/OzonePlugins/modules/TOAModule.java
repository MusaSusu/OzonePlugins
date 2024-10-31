package OzonePlugins.modules;

import OzonePlugins.components.invocations.InvocationPresetsManager;
import OzonePlugins.components.kephri.ScabarasPuzzle.*;
import OzonePlugins.components.zebak.CrondisPuzzle;
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
        //Zebak
        lifecycleComponents.addBinding().to(ZebakManager.class);
        lifecycleComponents.addBinding().to(CrondisPuzzle.class);

        //Kephri
        lifecycleComponents.addBinding().to(ScabarasManager.class);
        lifecycleComponents.addBinding().to(AdditionPuzzleSolver.class);
        lifecycleComponents.addBinding().to(LayoutConfigurer.class);
        lifecycleComponents.addBinding().to(SequencePuzzleSolver.class);
        lifecycleComponents.addBinding().to(LightPuzzleSolver.class);
        lifecycleComponents.addBinding().to(MatchingPuzzleSolver.class);

    }
}
