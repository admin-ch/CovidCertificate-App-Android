package ch.admin.bag.covidcertificate.common.debug
/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.databinding.FragmentDebugBinding
import ch.admin.bag.covidcertificate.sdk.android.net.CertificatePinning

open class DebugFragment : Fragment() {

	companion object {
		fun newInstance(): DebugFragment = DebugFragment()

		const val EXISTS = true

		fun initDebug(context: Context) {
			CertificatePinning.enabled = DebugSecureStorage.getInstance(context).isCertPinningEnabled
		}
	}

	private var _binding: FragmentDebugBinding? = null
	protected val binding get() = _binding!!

	private lateinit var debugSecureStorage: DebugSecureStorage

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		debugSecureStorage = DebugSecureStorage.getInstance(requireContext())
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentDebugBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.toolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		binding.buttonToggleCertificatePinning.apply {
			isChecked = debugSecureStorage.isCertPinningEnabled

			setOnCheckedChangeListener { _, isChecked ->
				debugSecureStorage.isCertPinningEnabled = isChecked
				CertificatePinning.enabled = isChecked
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}