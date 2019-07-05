package org.gfd.collect.caferedd.externalintents;

import androidx.test.filters.Suppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.gfd.collect.caferedd.activities.SplashScreenActivity;

import java.io.IOException;

import static org.gfd.collect.caferedd.externalintents.ExportedActivitiesUtils.testDirectories;

@Suppress
// Frequent failures: https://github.com/opendatakit/collect/issues/796
public class SplashScreenActivityTest {

    @Rule
    public ActivityTestRule<SplashScreenActivity> splashScreenActivityRule =
            new ExportedActivityTestRule<>(SplashScreenActivity.class);

    @Test
    public void splashScreenActivityMakesDirsTest() throws IOException {
        testDirectories();
    }

}
