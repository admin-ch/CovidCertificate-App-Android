/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.onboarding

import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.admin.bag.covidcertificate.common.onboarding.BaseOnboardingActivity
import ch.admin.bag.covidcertificate.common.onboarding.SimpleOnboardingPagerAdapter
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.onboarding.agbupdate.UpdateboardingAgbFragment
import ch.admin.bag.covidcertificate.wallet.onboarding.certificatelight.UpdateboardingCertificateLightFragment
import ch.admin.bag.covidcertificate.wallet.onboarding.validity.UpdateboardingValidity1Fragment
import ch.admin.bag.covidcertificate.wallet.onboarding.validity.UpdateboardingValidity2Fragment
import ch.admin.bag.covidcertificate.wallet.onboarding.validity.UpdateboardingValidity3Fragment
import ch.admin.bag.covidcertificate.wallet.onboarding.validity.UpdateboardingValidity4Fragment

class OnboardingActivity : BaseOnboardingActivity() {

	override fun getPagerAdapter(): FragmentStateAdapter {
		val onboardingType =
			OnboardingType.valueOf(intent.getStringExtra(EXTRA_ONBOARDING_TYPE) ?: OnboardingType.FRESH_INSTALL.name)

		return when (onboardingType) {
			OnboardingType.FRESH_INSTALL -> SimpleOnboardingPagerAdapter(
				this,
				SimpleOnboardingPagerAdapter.FragmentProvider { OnboardingIntroFragment.newInstance() },
				SimpleOnboardingPagerAdapter.FragmentProvider {
					OnboardingContentFragment.newInstance(
						R.string.wallet_onboarding_store_title,
						R.string.wallet_onboarding_store_header,
						R.drawable.illu_onboarding_privacy,
						R.string.wallet_onboarding_store_text1,
						R.drawable.ic_privacy,
						R.string.wallet_onboarding_store_text2,
						R.drawable.ic_validation
					)
				},
				SimpleOnboardingPagerAdapter.FragmentProvider {
					OnboardingContentFragment.newInstance(
						R.string.wallet_onboarding_show_title,
						R.string.wallet_onboarding_show_header,
						R.drawable.illu_onboarding_covid_certificate,
						R.string.wallet_onboarding_show_text1,
						R.drawable.ic_qr_certificate,
						R.string.wallet_onboarding_show_text2,
						R.drawable.ic_check_mark
					)
				},
				SimpleOnboardingPagerAdapter.FragmentProvider { OnboardingAgbFragment.newInstance() },
			)
			OnboardingType.CERTIFICATE_LIGHT -> SimpleOnboardingPagerAdapter(
				this,
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingCertificateLightFragment.newInstance() }
			)
			OnboardingType.AGB_UPDATE -> SimpleOnboardingPagerAdapter(
				this,
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingAgbFragment.newInstance() }
			)
			OnboardingType.VALIDITY -> SimpleOnboardingPagerAdapter(
				this,
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingValidity1Fragment.newInstance() },
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingValidity2Fragment.newInstance() },
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingValidity3Fragment.newInstance() },
				SimpleOnboardingPagerAdapter.FragmentProvider { UpdateboardingValidity4Fragment.newInstance() },
			)
		}
	}

	enum class OnboardingType {
		FRESH_INSTALL,
		CERTIFICATE_LIGHT,
		AGB_UPDATE,
		VALIDITY,
	}

}