package ch.admin.bag.covidcertificate.wallet


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import ch.admin.bag.covidcertificate.common.browserstack.AirplaneMode
import ch.admin.bag.covidcertificate.common.browserstack.BadNetwork
import ch.admin.bag.covidcertificate.common.browserstack.Normal
import org.hamcrest.Matchers.allOf
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@Normal
@BadNetwork
@AirplaneMode
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OnboardingTest : EspressoUtil() {

	@Rule
	@JvmField
	var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

	@Test
	fun A_onboardingTest() {
		doOnboarding()

		val textView = onView(
			allOf(
				withId(R.id.homescreen_add_certificate_options_title),
				isDisplayed()
			)
		)
		textView.check(matches(withText(R.string.wallet_homescreen_what_to_do)))
	}


	@Test
	fun B_onboardingTestShowNoOnboarding() {
		val textView = onView(
			allOf(
				withId(R.id.homescreen_add_certificate_options_title),
				isDisplayed()
			)
		)
		textView.check(matches(withText(R.string.wallet_homescreen_what_to_do)))
	}
}
