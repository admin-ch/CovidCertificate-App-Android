/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.onboarding.certificatelight

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UpdateboardingCertificateLightSlidePageAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
	override fun createFragment(position: Int): Fragment {
		when (position) {
			0 -> return UpdateboardingCertificateLightFragment.newInstance()
		}
		throw IllegalArgumentException("There is no fragment for view pager position $position")
	}

	override fun getItemCount() = 1
}