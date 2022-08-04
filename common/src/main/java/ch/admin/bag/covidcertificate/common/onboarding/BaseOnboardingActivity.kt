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

import android.os.Bundle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.admin.bag.covidcertificate.common.BaseActivity
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.databinding.ActivityOnboardingBinding

abstract class BaseOnboardingActivity : BaseActivity() {

	companion object {
		const val EXTRA_ONBOARDING_TYPE = "EXTRA_ONBOARDING_TYPE"
	}

	private lateinit var binding: ActivityOnboardingBinding
	private lateinit var pagerAdapter: FragmentStateAdapter

	protected abstract fun getPagerAdapter(): FragmentStateAdapter?

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val pagerAdapter = getPagerAdapter()
		if (pagerAdapter == null) {
			finish()
			return
		}

		binding = ActivityOnboardingBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.viewPager.isUserInputEnabled = false

		this.pagerAdapter = pagerAdapter
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


}