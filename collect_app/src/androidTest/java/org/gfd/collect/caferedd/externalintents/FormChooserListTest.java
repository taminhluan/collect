package org.gfd.collect.caferedd.externalintents;

import androidx.test.filters.Suppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.gfd.collect.caferedd.activities.FormChooserList;

import java.io.IOException;

import static org.gfd.collect.caferedd.externalintents.ExportedActivitiesUtils.testDirectories;

@Suppress
// Frequent failures: https://github.com/opendatakit/collect/issues/796
public class FormChooserListTest {

    @Rule
    public ActivityTestRule<FormChooserList> formChooserListRule =
            new ExportedActivityTestRule<>(FormChooserList.class);

    @Test
    public void formChooserListMakesDirsTest() throws IOException {
        testDirectories();
    }

}
