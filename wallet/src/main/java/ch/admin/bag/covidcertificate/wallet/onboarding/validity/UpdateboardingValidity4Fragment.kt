/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.onboarding.validity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentUpdateboardingValidity4Binding
import ch.admin.bag.covidcertificate.wallet.onboarding.OnboardingActivity

class UpdateboardingValidity4Fragment : Fragment() {

	companion object {
		fun newInstance(): UpdateboardingValidity4Fragment {
			return UpdateboardingValidity4Fragment()
		}
	}

	private var _binding: FragmentUpdateboardingValidity4Binding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentUpdateboardingValidity4Binding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.onboardingContinueButton.setOnClickListener {
			(requireActivity() as OnboardingActivity).continueToNextPage()
		}

		binding.link1.setOnClickListener { v ->
			val url = getString(R.string.wallet_update_boarding_page_4_link_1_url)
			UrlUtil.openUrl(v.context, url)
		}
		binding.link2.setOnClickListener { v ->
			val url = getString(R.string.wallet_update_boarding_page_4_link_2_url)
			UrlUtil.openUrl(v.context, url)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}
