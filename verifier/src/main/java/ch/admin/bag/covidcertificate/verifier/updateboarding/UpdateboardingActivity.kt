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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.ActivityUpdateboardingBinding

class UpdateboardingActivity : AppCompatActivity() {

	private lateinit var binding: ActivityUpdateboardingBinding
	private lateinit var pagerAdapter: FragmentStateAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityUpdateboardingBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.viewPager.isUserInputEnabled = false

		pagerAdapter = UpdateboardingPagerAdapter(this)
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