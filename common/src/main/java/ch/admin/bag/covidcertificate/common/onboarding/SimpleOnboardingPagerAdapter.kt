/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SimpleOnboardingPagerAdapter(
	fragmentActivity: FragmentActivity,
	private vararg val fragmentProviders: FragmentProvider
) : FragmentStateAdapter(fragmentActivity) {

	override fun createFragment(position: Int) = fragmentProviders[position].provide()

	override fun getItemCount() = fragmentProviders.size

	fun interface FragmentProvider {
		fun provide(): Fragment
	}
}