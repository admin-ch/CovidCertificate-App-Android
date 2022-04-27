/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers

open class EspressoUtil {

	fun doOnboarding() {
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
		Espresso.onIdle()
		Thread.sleep(500)
		try{
			val materialButton5 = Espresso.onView(
				Matchers.allOf(
					ViewMatchers.withId(R.id.info_dialog_close_button),
					ViewMatchers.isDisplayed()
				)
			)
			materialButton5.perform(ViewActions.click())
		}catch(e: Exception){

		}
	}

	// The standard scrollTo Action does not support NestedScrollView. This implementation does support NestedScrollView in
	// addition to the Views supported by the standard ScrollToAction
	fun scrollTo(): ViewAction {
		return ViewActions.actionWithAssertions(NestedScrollToAction())
	}
}
