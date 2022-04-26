/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.ratconversion

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentRatConversionBinding

class RatConversionFragment : Fragment(R.layout.fragment_rat_conversion) {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificateHolder: CertificateHolder) = RatConversionFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificateHolder)
		}
	}

	private var _binding: FragmentRatConversionBinding? = null
	private val binding get() = _binding!!

	private val certificatesViewModel by activityViewModels<CertificatesAndConfigViewModel>()

	private lateinit var certificateHolder: CertificateHolder
	private var isFormDataTransferChecked = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? CertificateHolder)
			?: throw IllegalStateException("RatConversionFragment created without Certificate!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentRatConversionBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		setupForm()
		updateForm()
	}

	private fun setupForm() {
		binding.ratConversionFormCheckbox.setOnClickListener {
			isFormDataTransferChecked = !isFormDataTransferChecked
			updateForm()
		}

		binding.ratConversionFormSubmit.setOnClickListener {
			val url = ConfigRepository.getCurrentConfig(requireContext())?.ratConversionFormUrl ?: return@setOnClickListener
			val hcert = certificatesViewModel.getRawHcertForCertificateHolder(certificateHolder) ?: return@setOnClickListener
			val encodedHcert = Uri.encode(hcert, Charsets.UTF_8.name())
			val finalUrl = "$url#hcert=$encodedHcert"
			UrlUtil.openUrl(requireContext(), finalUrl)
		}

		binding.openWebsiteButton.setOnClickListener {
			val url = ConfigRepository.getCurrentConfig(requireContext())?.ratConversionFormUrl ?: return@setOnClickListener
			UrlUtil.openUrl(requireContext(), url)
		}
	}

	private fun updateForm() {
		if (isFormDataTransferChecked) {
			binding.ratConversionFormCheckboxIcon.setImageResource(R.drawable.ic_checkbox_filled)
			binding.ratConversionFormCheckboxIcon.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.blue, null))
		} else {
			binding.ratConversionFormCheckboxIcon.setImageResource(R.drawable.ic_checkbox_empty)
			binding.ratConversionFormCheckboxIcon.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.grey, null))
		}
		binding.ratConversionFormSubmit.isEnabled = isFormDataTransferChecked
	}

}