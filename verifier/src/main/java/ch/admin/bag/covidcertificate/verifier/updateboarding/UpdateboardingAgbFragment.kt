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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentUpdateboardingAgbBinding

class UpdateboardingAgbFragment : Fragment(R.layout.fragment_updateboarding_agb) {

	companion object {
		fun newInstance() = UpdateboardingAgbFragment()
	}

	private var _binding: FragmentUpdateboardingAgbBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentUpdateboardingAgbBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.onboardingContinueButton.setOnClickListener {
			(requireActivity() as UpdateboardingActivity).continueToNextPage()
		}

		binding.itemAgbLink.setOnClickListener { v ->
			val url = v.context.getString(R.string.verifier_terms_privacy_link)
			UrlUtil.openUrl(v.context, url)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}