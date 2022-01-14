package ch.admin.bag.covidcertificate.wallet

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers

open class EspressoUtil {

	fun doOnboarding(){
		val materialButton = Espresso.onView(
			Matchers.allOf(
				ViewMatchers.withId(R.id.onboarding_continue_button),
				ViewMatchers.isDisplayed()
			)
		)
		materialButton.perform(ViewActions.click())
		val materialButton2 = Espresso.onView(
			Matchers.allOf(
				ViewMatchers.withId(R.id.onboarding_continue_button),
				ViewMatchers.isDisplayed()
			)
		)
		materialButton2.perform(ViewActions.click())


		val materialButton3 = Espresso.onView(
			Matchers.allOf(
				ViewMatchers.withId(R.id.onboarding_continue_button),
				ViewMatchers.isDisplayed()
			)
		)
		materialButton3.perform(ViewActions.click())

		val materialButton4 = Espresso.onView(
			Matchers.allOf(
				ViewMatchers.withId(R.id.onboarding_continue_button),
				ViewMatchers.isDisplayed()
			)
		)
		materialButton4.perform(ViewActions.click())
	}
}
