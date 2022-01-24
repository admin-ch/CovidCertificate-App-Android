package ch.admin.bag.covidcertificate.wallet


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import ch.admin.bag.covidcertificate.common.browserstack.AirplaneMode
import ch.admin.bag.covidcertificate.common.browserstack.BadNetwork
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@AirplaneMode
@BadNetwork
@LargeTest
@RunWith(AndroidJUnit4::class)
class LoadconfigTest : EspressoUtil() {

	@Rule
	@JvmField
	var mActivityTestRule = ActivityTestRule(MainActivity::class.java)
	val secureStorage by lazy { WalletSecureStorage.getInstance(InstrumentationRegistry.getInstrumentation().getTargetContext()) }

	@Before
	fun checkIfOnboardingIsShown(){
		if (secureStorage.getOnboardingCompleted()){
			return
		}
		doOnboarding()
	}

	@Test
	fun loadconfigTest() {
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
