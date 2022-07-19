/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.updateboarding

import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.admin.bag.covidcertificate.common.onboarding.BaseOnboardingActivity
import ch.admin.bag.covidcertificate.common.onboarding.SimpleOnboardingPagerAdapter

class UpdateboardingActivity : BaseOnboardingActivity() {

	override fun getPagerAdapter(): FragmentStateAdapter? {
		val onboardingType = intent.getStringExtra(EXTRA_ONBOARDING_TYPE)?.let { OnboardingType.valueOf(it) }

		return when (onboardingType) {
			OnboardingType.CERTIFICATE_LIGHT -> SimpleOnboardingPagerAdapter(
				this,
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingCertificateLightFragment.newInstance() }
			)
			OnboardingType.AGB_UPDATE -> SimpleOnboardingPagerAdapter(
				this,
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingAgbFragment.newInstance() }
			)
			else -> null
		}
	}

	enum class OnboardingType {
		CERTIFICATE_LIGHT, AGB_UPDATE
	}
}