package ch.admin.bag.covidcertificate.wallet


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class TransferCodeTest : EspressoUtil() {


	private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
	private val secureStorage by lazy { WalletSecureStorage.getInstance(context) }

	@Before
	fun setup() {
		if (!secureStorage.getOnboardingCompleted()) doOnboarding()
	}

	@Rule
	@JvmField
	var mActivityTestRule = ActivityTestRule(MainActivity::class.java)


	@Test
	fun testCreateTransferCode() {

		Assert.assertEquals(0, countTransfercodesOnHomeScreen())
		createTransfercode()
		Assert.assertEquals(1, countTransfercodesOnHomeScreen())
		deleteTransfercode()
		Assert.assertEquals(0, countTransfercodesOnHomeScreen())

	}

	@Test
	fun testCreateMultipleTransferCodes() {

		Assert.assertEquals(0, countTransfercodesOnHomeScreen())
		createTransfercode()
		Assert.assertEquals(1, countTransfercodesOnHomeScreen())
		createTransfercode()
		Assert.assertEquals(2, countTransfercodesOnHomeScreen())
		deleteTransfercode()
		Assert.assertEquals(1, countTransfercodesOnHomeScreen())
		deleteTransfercode()
		Assert.assertEquals(0, countTransfercodesOnHomeScreen())

	}


	private fun clickButton(id: Int) {
		onView(allOf(withId(id), isDisplayed())).perform(click())
	}

	private fun createTransfercode() {

		if (countTransfercodesOnHomeScreen() != 0) {
			clickButton(R.id.homescreen_scan_button_small)
		}

		clickButton(R.id.option_transfer_code)

		clickButton(R.id.transfer_code_intro_create)

		Thread.sleep(1000)
		onView(withId(R.id.transfer_code_content)).perform(waitUntilVisible(30000L))

		clickButton(R.id.transfer_code_creation_done_button)

	}

	private fun deleteTransfercode() {

		onView(allOf(withId(R.id.transfer_code_page_card), isCompletelyDisplayed())).perform(click())


		Thread.sleep(1000)
		onView(withId(R.id.transfer_code_content)).perform(waitUntilVisible(30000L))

		onView(withId(R.id.transfer_code_content)).perform(swipeUp())

		clickButton(R.id.transfer_code_detail_delete_button)
		clickButton(android.R.id.button1)

	}

	private fun countTransfercodesOnHomeScreen(): Int {

		val recyclerView = onView(
			allOf(
				withId(R.id.homescreen_certificates_view_pager),
				isDisplayed()
			)
		)

		var count = 0
		recyclerView.check { view, _ ->
			if (view == null) return@check
			count = (view as ViewPager2).adapter?.itemCount ?: 0
		}
		return count
	}

	/**
	 * @return a [WaitUntilVisibleAction] instance created with the given [timeout] parameter.
	 */
	private fun waitUntilVisible(timeout: Long): ViewAction = WaitUntilVisibleAction(timeout)

}
