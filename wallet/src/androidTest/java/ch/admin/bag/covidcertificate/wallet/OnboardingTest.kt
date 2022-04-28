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


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import ch.admin.bag.covidcertificate.common.browserstack.Onboarding
import org.hamcrest.Matchers.allOf
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@Onboarding
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
