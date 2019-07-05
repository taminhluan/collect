package org.gfd.collect.caferedd;

import androidx.test.rule.ActivityTestRule;
import android.text.TextUtils;

import org.javarosa.form.api.FormEntryPrompt;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.gfd.collect.caferedd.activities.FormEntryActivity;
import org.gfd.collect.caferedd.application.Collect;
import org.gfd.collect.caferedd.preferences.GeneralSharedPreferences;
import org.gfd.collect.caferedd.preferences.GuidanceHint;
import org.gfd.collect.caferedd.preferences.GeneralKeys;
import org.gfd.collect.caferedd.test.FormLoadingUtils;

import java.io.IOException;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.not;

public class GuidanceHintFormTest {
    private static final String GUIDANCE_SAMPLE_FORM = "guidance_hint_form.xml";

    @Rule
    public ActivityTestRule<FormEntryActivity> activityTestRule = FormLoadingUtils.getFormActivityTestRuleFor(GUIDANCE_SAMPLE_FORM);

    //region Test prep.
    @BeforeClass
    public static void copyFormToSdCard() throws IOException {
        FormLoadingUtils.copyFormToSdCard(GUIDANCE_SAMPLE_FORM);
    }

    @BeforeClass
    public static void beforeAll() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
    }

    @Before
    public void resetPreferences() {
        GeneralSharedPreferences.getInstance().reloadPreferences();
    }

    @AfterClass
    public static void resetPreferencesAtEnd() {
        GeneralSharedPreferences.getInstance().reloadPreferences();
    }

    @Test
    public void guidanceHint_ShouldBeHiddenByDefault() {
        onView(withId(R.id.guidance_text_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void guidanceHint_ShouldBeDisplayedWhenSettingSetToYes() {
        GeneralSharedPreferences.getInstance().save(GeneralKeys.KEY_GUIDANCE_HINT, GuidanceHint.Yes.toString());
        // jump to force recreation of the view after the settings change
        onView(withId(R.id.menu_goto)).perform(click());
        onView(withId(R.id.jumpBeginningButton)).perform(click());

        FormEntryPrompt prompt = Collect.getInstance().getFormController().getQuestionPrompt();
        String guidance = prompt.getSpecialFormQuestionText(prompt.getQuestion().getHelpTextID(), "guidance");
        assertFalse(TextUtils.isEmpty(guidance));

        Screengrab.screenshot("guidance_hint");

        onView(withId(R.id.guidance_text_view)).check(matches(withText(guidance)));
    }

    @Test
    public void guidanceHint_ShouldBeDisplayedAfterClickWhenSettingSetToYesCollapsed() {
        GeneralSharedPreferences.getInstance().save(GeneralKeys.KEY_GUIDANCE_HINT, GuidanceHint.YesCollapsed.toString());
        // jump to force recreation of the view after the settings change
        onView(withId(R.id.menu_goto)).perform(click());
        onView(withId(R.id.jumpBeginningButton)).perform(click());

        FormEntryPrompt prompt = Collect.getInstance().getFormController().getQuestionPrompt();
        String guidance = prompt.getSpecialFormQuestionText(prompt.getQuestion().getHelpTextID(), "guidance");
        assertFalse(TextUtils.isEmpty(guidance));

        onView(withId(R.id.guidance_text_view)).check(matches(not(isDisplayed())));
        onView(withId(R.id.help_icon)).perform(click());
        onView(withId(R.id.guidance_text_view)).check(matches(withText(guidance)));
    }
}
