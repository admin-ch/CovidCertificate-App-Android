/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodeIntroBinding

class TransferCodeIntroFragment : Fragment(R.layout.fragment_transfer_code_intro) {

	companion object {
		fun newInstance() = TransferCodeIntroFragment()
	}

	private var _binding: FragmentTransferCodeIntroBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentTransferCodeIntroBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		binding.transferCodeIntroCreate.setOnClickListener {
			// TODO: Open tranfer code creation screen
		}

		binding.transferCodeIntroHowto.setOnClickListener {
			// TODO: Open FAQ screen for transfer codes
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}