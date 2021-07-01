/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.light

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.DccHolder
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificateLightConversionBinding

class CertificateLightConversionFragment : Fragment(R.layout.fragment_certificate_light_conversion) {

	companion object {
		private const val ARG_DCC_HOLDER = "ARG_DCC_HOLDER"

		fun newInstance(dccHolder: DccHolder) = CertificateLightConversionFragment().apply {
			arguments = bundleOf(ARG_DCC_HOLDER to dccHolder)
		}
	}

	private var _binding: FragmentCertificateLightConversionBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dccHolder = (arguments?.getSerializable(ARG_DCC_HOLDER) as? DccHolder)
			?: throw IllegalStateException("Certificate light fragment created without a DccHolder!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentCertificateLightConversionBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}