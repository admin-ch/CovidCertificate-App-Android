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

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

class NestedScrollToAction(scrolltoAction: ViewAction = ViewActions.scrollTo()) : ViewAction by scrolltoAction {
	override fun getConstraints(): Matcher<View> {
		return Matchers.allOf(
			ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
			ViewMatchers.isDescendantOfA(
				Matchers.anyOf(
					ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
					ViewMatchers.isAssignableFrom(ScrollView::class.java),
					ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
					ViewMatchers.isAssignableFrom(ListView::class.java)
				)
			)
		)
	}
}

