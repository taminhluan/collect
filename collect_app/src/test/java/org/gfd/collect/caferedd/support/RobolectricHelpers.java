package org.gfd.collect.caferedd.support;

import org.gfd.collect.caferedd.application.Collect;
import org.gfd.collect.caferedd.injection.config.AppDependencyComponent;
import org.gfd.collect.caferedd.injection.config.AppDependencyModule;
import org.gfd.collect.caferedd.injection.config.DaggerAppDependencyComponent;
import org.robolectric.RuntimeEnvironment;

public class RobolectricHelpers {

    private RobolectricHelpers() {}

    public static void overrideAppDependencyModule(AppDependencyModule appDependencyModule) {
        AppDependencyComponent testComponent = DaggerAppDependencyComponent.builder()
                .application(RuntimeEnvironment.application)
                .appDependencyModule(appDependencyModule)
                .build();
        ((Collect) RuntimeEnvironment.application).setComponent(testComponent);
    }

    public static AppDependencyComponent getApplicationComponent() {
        return ((Collect) RuntimeEnvironment.application).getComponent();
    }
}
