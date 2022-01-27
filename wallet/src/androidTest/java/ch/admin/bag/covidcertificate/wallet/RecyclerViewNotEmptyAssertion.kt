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
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import org.junit.Assert.assertNotNull


class RecyclerViewNotEmptyAssertion : ViewAssertion {
	override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
		if (noViewFoundException != null) {
			throw noViewFoundException
		}
		val recyclerView = view as RecyclerView
		val adapter = recyclerView.adapter
		assertNotNull(adapter)
		assert(adapter!!.itemCount > 0)
	}
}

