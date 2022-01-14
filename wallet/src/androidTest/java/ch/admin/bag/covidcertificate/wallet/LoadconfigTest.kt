package ch.admin.bag.covidcertificate.wallet


import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import ch.admin.bag.covidcertificate.common.browserstack.AirplaneMode
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@AirplaneMode
@LargeTest
@RunWith(AndroidJUnit4::class)
class LoadconfigTest {

	@Rule
	@JvmField
	var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

	@Test
	fun loadconfigTest() {
        val materialButton = onView(
            allOf(
                withId(R.id.onboarding_continue_button),
                isDisplayed()
            )
        )
        materialButton.perform(click())
        val materialButton2 = onView(
            allOf(
                withId(R.id.onboarding_continue_button),
                isDisplayed()
            )
        )
        materialButton2.perform(click())


        val materialButton3 = onView(
            allOf(
                withId(R.id.onboarding_continue_button),
                isDisplayed()
            )
        )
        materialButton3.perform(click())

        val materialButton4 = onView(
            allOf(
                withId(R.id.onboarding_continue_button),
                isDisplayed()
            )
        )
        materialButton4.perform(click())

        val supportButton = onView(
            allOf(
                withId(R.id.homescreen_support_button),
                isDisplayed()
            )
        )
        supportButton.perform(click())

		onView(
			allOf(
				withId(R.id.faq_recycler_view),
				isDisplayed(),
			)
		).check( RecyclerViewNotEmptyAssertion())
	}
}
