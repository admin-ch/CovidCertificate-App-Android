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
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentUpdateboardingValidity2Binding
import ch.admin.bag.covidcertificate.wallet.onboarding.OnboardingActivity

class UpdateboardingValidity2Fragment : Fragment() {

	companion object {
		fun newInstance(): UpdateboardingValidity2Fragment {
			return UpdateboardingValidity2Fragment()
		}
	}

	private var _binding: FragmentUpdateboardingValidity2Binding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentUpdateboardingValidity2Binding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.onboardingContinueButton.setOnClickListener {
			(requireActivity() as OnboardingActivity).continueToNextPage()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}
