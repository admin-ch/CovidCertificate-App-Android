/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.homescreen.pager

import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.util.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.common.util.parseIsoTimeAndFormat
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificatePagerBinding
import ch.admin.bag.covidcertificate.wallet.util.QrCode
import ch.admin.bag.covidcertificate.wallet.util.getInfoBubbleColor
import ch.admin.bag.covidcertificate.wallet.util.getNameDobColor
import ch.admin.bag.covidcertificate.wallet.util.getQrAlpha
import ch.admin.bag.covidcertificate.wallet.util.getStatusIcon
import ch.admin.bag.covidcertificate.wallet.util.getStatusString

class CertificatePagerFragment : Fragment() {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder) = CertificatePagerFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificatePagerBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dccHolder = arguments?.getSerializable(ARG_CERTIFICATE) as? DccHolder
			?: throw IllegalStateException("Certificate pager fragment created without QrCode!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificatePagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val qrCodeBitmap = QrCode.renderToBitmap(dccHolder.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificatePageQrCode.setImageDrawable(qrCodeDrawable)

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificatePageName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.parseIsoTimeAndFormat(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificatePageBirthdate.text = dateOfBirth

		setupStatusInfo()

		binding.certificatePageMainGroup.setOnClickListener { certificatesViewModel.onQrCodeClicked(dccHolder) }
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupStatusInfo() {
		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { certificates ->
			certificates.find { it.dccHolder == dccHolder }?.let {
				updateStatusInfo(it.state)
			}
		}

		certificatesViewModel.startVerification(dccHolder)
	}

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return
		val context = binding.root.context

		binding.certificatePageName.setTextColor(ContextCompat.getColor(context, state.getNameDobColor()))
		binding.certificatePageBirthdate.setTextColor(ContextCompat.getColor(context, state.getNameDobColor()))
		binding.certificatePageQrCode.alpha = state.getQrAlpha()
		binding.certificatePageInfo.backgroundTintList =
			ColorStateList.valueOf(ContextCompat.getColor(context, state.getInfoBubbleColor()))
		binding.certificatePageStatusIcon.setImageResource(state.getStatusIcon())

		binding.certificatePageInfo.text = state.getStatusString(context)

		when (state) {
			is VerificationState.INVALID, is VerificationState.SUCCESS, is VerificationState.ERROR -> {
				binding.certificatePageStatusLoading.isVisible = false
				binding.certificatePageStatusIcon.isVisible = true
			}
			VerificationState.LOADING -> {
				binding.certificatePageStatusLoading.isVisible = true
				binding.certificatePageStatusIcon.isVisible = false
			}
		}
	}
}