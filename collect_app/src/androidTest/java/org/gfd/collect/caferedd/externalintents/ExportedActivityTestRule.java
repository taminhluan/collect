package org.gfd.collect.caferedd.externalintents;

import android.app.Activity;
import androidx.test.rule.ActivityTestRule;

import static org.gfd.collect.caferedd.externalintents.ExportedActivitiesUtils.clearDirectories;

class ExportedActivityTestRule<A extends Activity> extends ActivityTestRule<A> {

    ExportedActivityTestRule(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();

        clearDirectories();
    }

}
