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
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.common.views.setCutOutCardBackground
import ch.admin.bag.covidcertificate.sdk.core.extensions.isNotFullyProtected
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificatePagerBinding
import ch.admin.bag.covidcertificate.wallet.util.QrCode
import ch.admin.bag.covidcertificate.wallet.util.getNameDobColor
import ch.admin.bag.covidcertificate.wallet.util.getQrAlpha
import ch.admin.bag.covidcertificate.wallet.util.getValidationStatusString
import ch.admin.bag.covidcertificate.wallet.util.isOfflineMode

class CertificatePagerFragment : Fragment() {

	companion object {
		private const val ARG_QR_CODE_DATA = "ARG_QR_CODE_DATA"
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(qrCodeData: String, certificateHolder: CertificateHolder?) = CertificatePagerFragment().apply {
			arguments = bundleOf(ARG_QR_CODE_DATA to qrCodeData, ARG_CERTIFICATE to certificateHolder)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificatePagerBinding? = null
	private val binding get() = _binding!!

	private lateinit var qrCodeData: String
	private var certificateHolder: CertificateHolder? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.let { args ->
			qrCodeData = args.getString(ARG_QR_CODE_DATA)
				?: throw IllegalStateException("Certificate pager fragment created without QrCode!")
			certificateHolder = args.getSerializable(ARG_CERTIFICATE) as? CertificateHolder?
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificatePagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val qrCodeBitmap = QrCode.renderToBitmap(qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificatePageQrCode.setImageDrawable(qrCodeDrawable)

		val name = certificateHolder?.certificate?.getPersonName()?.let { "${it.familyName} ${it.givenName}" }
		binding.certificatePageName.text = name
		binding.certificatePageBirthdate.text = certificateHolder?.certificate?.getFormattedDateOfBirth()
		binding.certificatePageCard.setCutOutCardBackground()
		updateTitle()
		setupStatusInfo()

		certificateHolder?.let { certificate ->
			binding.certificatePageCard.setOnClickListener { certificatesViewModel.onQrCodeClicked(certificate) }
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun updateTitle() {
		val dccCert = certificateHolder?.certificate as? DccCert

		val vaccinationEntry: VaccinationEntry? = dccCert?.vaccinations?.firstOrNull()
		if (vaccinationEntry?.isNotFullyProtected() == true) {
			binding.certificatePageTitle.setText(R.string.wallet_certificate_evidence_title)
		} else {
			binding.certificatePageTitle.setText(R.string.covid_certificate_title)
		}
	}

	private fun setupStatusInfo() {
		certificatesViewModel.statefulWalletItems.observe(viewLifecycleOwner) { items ->
			items.filterIsInstance(StatefulWalletItem.VerifiedCertificate::class.java)
				.find { it.qrCodeData == qrCodeData }?.let {
					updateStatusInfo(it.state)
				}
		}

		certificateHolder?.let { certificatesViewModel.startVerification(it) }
	}

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return

		when (state) {
			is VerificationState.LOADING -> displayLoadingState()
			is VerificationState.SUCCESS -> displaySuccessState(state)
			is VerificationState.INVALID -> displayInvalidState(state)
			is VerificationState.ERROR -> displayErrorState(state)
		}

		setCertificateDetailTextColor(state.getNameDobColor())
		binding.certificatePageQrCode.alpha = state.getQrAlpha()
	}

	private fun displayLoadingState() {
		val context = context ?: return
		showLoadingIndicator(true)
		setInfoBubbleBackground(R.color.greyish)
		binding.certificatePageStatusIcon.setImageResource(0)
		binding.certificatePageInfoRedBorder.visibility = View.GONE
		binding.certificatePageInfo.text = SpannableString(context.getString(R.string.wallet_certificate_verifying))
	}

	private fun displaySuccessState(state: VerificationState.SUCCESS) {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.blueish)
		if(state.isValidOnlyInSwitzerland){
			binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_flag_ch)
			binding.certificatePageInfoRedBorder.visibility = View.VISIBLE
			binding.certificatePageInfo.text = SpannableString(context.getString(R.string.wallet_only_valid_in_switzerland))
		}else {
			binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_info_blue)
			binding.certificatePageInfoRedBorder.visibility = View.GONE
			binding.certificatePageInfo.text = SpannableString(context.getString(R.string.verifier_verify_success_info))
		}
	}

	private fun displayInvalidState(state: VerificationState.INVALID) {
		val context = context ?: return
		showLoadingIndicator(false)

		val infoBubbleColorId: Int
		val statusIconId: Int
		val signatureState = state.signatureState
		val revocationState = state.revocationState
		val nationalRulesState = state.nationalRulesState

		when {
			signatureState is CheckSignatureState.INVALID -> {
				infoBubbleColorId = R.color.greyish
				statusIconId = R.drawable.ic_error_grey
			}
			revocationState is CheckRevocationState.INVALID -> {
				infoBubbleColorId = R.color.greyish
				statusIconId = R.drawable.ic_error_grey
			}
			nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
				infoBubbleColorId = R.color.blueish
				statusIconId = R.drawable.ic_invalid_grey
			}
			nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> {
				infoBubbleColorId = R.color.blueish
				statusIconId = R.drawable.ic_timelapse
			}
			else -> {
				infoBubbleColorId = R.color.greyish
				statusIconId = R.drawable.ic_error_grey
			}
		}

		setInfoBubbleBackground(infoBubbleColorId)
		binding.certificatePageStatusIcon.setImageResource(statusIconId)
		binding.certificatePageInfoRedBorder.visibility = View.GONE
		binding.certificatePageInfo.text = state.getValidationStatusString(context)
	}

	private fun displayErrorState(state: VerificationState.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		setInfoBubbleBackground(R.color.greyish)

		val statusIconId = if (state.isOfflineMode()) R.drawable.ic_offline else R.drawable.ic_process_error_grey
		binding.certificatePageStatusIcon.setImageResource(statusIconId)
		binding.certificatePageInfoRedBorder.visibility = View.GONE

		binding.certificatePageInfo.text = if (state.isOfflineMode()) {
			context.getString(R.string.wallet_homescreen_offline).makeBold()
		} else {
			SpannableString(context.getString(R.string.wallet_homescreen_network_error))
		}
	}

	private fun showLoadingIndicator(isLoading: Boolean) {
		binding.certificatePageStatusLoading.isVisible = isLoading
		binding.certificatePageStatusIcon.isVisible = !isLoading
	}

	private fun setInfoBubbleBackground(@ColorRes infoBubbleColorId: Int) {
		val infoBubbleColor = ContextCompat.getColor(requireContext(), infoBubbleColorId)
		binding.certificatePageInfo.backgroundTintList = ColorStateList.valueOf(infoBubbleColor)
	}

	private fun setCertificateDetailTextColor(@ColorRes colorId: Int) {
		val textColor = ContextCompat.getColor(requireContext(), colorId)
		binding.certificatePageName.setTextColor(textColor)
		binding.certificatePageBirthdate.setTextColor(textColor)
	}

}