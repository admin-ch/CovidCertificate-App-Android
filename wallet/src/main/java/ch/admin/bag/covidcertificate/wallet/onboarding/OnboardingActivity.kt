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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.admin.bag.covidcertificate.common.BaseActivity
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.ActivityOnboardingBinding
import ch.admin.bag.covidcertificate.wallet.onboarding.update.UpdateboardingCertificateLightSlidePageAdapter

class OnboardingActivity : BaseActivity() {

	companion object {
		const val EXTRA_ONBOARDING_TYPE = "EXTRA_ONBOARDING_TYPE"
	}

	private lateinit var binding: ActivityOnboardingBinding
	private lateinit var pagerAdapter: FragmentStateAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val onboardingType =
			OnboardingType.valueOf(intent.getStringExtra(EXTRA_ONBOARDING_TYPE) ?: OnboardingType.FRESH_INSTALL.name)

		binding = ActivityOnboardingBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.viewPager.isUserInputEnabled = false

		pagerAdapter = when (onboardingType) {
			OnboardingType.FRESH_INSTALL -> OnboardingSlidePageAdapter(this)
			OnboardingType.CERTIFICATE_LIGHT -> UpdateboardingCertificateLightSlidePageAdapter(this)
		}
		binding.viewPager.adapter = pagerAdapter
	}

	fun continueToNextPage() {
		val currentItem: Int = binding.viewPager.currentItem
		if (currentItem < pagerAdapter.itemCount - 1) {
			binding.viewPager.setCurrentItem(currentItem + 1, true)
		} else {
			setResult(RESULT_OK)
			finish()
			overridePendingTransition(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
		}
	}

	enum class OnboardingType {
		FRESH_INSTALL, CERTIFICATE_LIGHT
	}

}