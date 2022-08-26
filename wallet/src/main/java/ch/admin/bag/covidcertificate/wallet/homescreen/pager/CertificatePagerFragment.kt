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
import android.graphics.Color
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
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.common.views.setCutOutCardBackground
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.extensions.isNotFullyProtected
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.state.*
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificatePagerBinding
import ch.admin.bag.covidcertificate.wallet.util.*

class CertificatePagerFragment : Fragment() {

	companion object {
		private const val ARG_QR_CODE_DATA = "ARG_QR_CODE_DATA"
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(qrCodeData: String, certificateHolder: CertificateHolder?) = CertificatePagerFragment().apply {
			arguments = bundleOf(ARG_QR_CODE_DATA to qrCodeData, ARG_CERTIFICATE to certificateHolder)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesAndConfigViewModel>()

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

		val name = certificateHolder?.certificate?.getPersonName()?.prettyName() ?: ""
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
				.find { it.qrCodeData == qrCodeData }?.let { verifiedCert ->
					val currentConfig = ConfigRepository.getCurrentConfig(requireContext())
					val state = cheatUiOnCertsExpiredInSwitzerland(currentConfig, verifiedCert.state)
					updateStatusInfo(state)
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
		binding.certificatePageQrCode.alpha = state.getInvalidQrCodeAlpha(certificateHolder?.certType == CertType.TEST)
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
		val walletState = state.successState as SuccessState.WalletSuccessState
		if (walletState.isValidOnlyInSwitzerland) {
			binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_flag_ch)
			binding.certificatePageInfoRedBorder.visibility = View.VISIBLE
			binding.certificatePageInfo.text = SpannableString(context.getString(R.string.wallet_only_valid_in_switzerland))
		} else {
			binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_info_blue)
			binding.certificatePageInfoRedBorder.visibility = View.GONE
			binding.certificatePageInfo.text = SpannableString(context.getString(R.string.verifier_verify_success_info))
		}

		binding.certificatePageBanner.isVisible = false
		binding.certificatePageRenewalBanner.isVisible = false
		val isRenewalBannerDisplayed = displayQrCodeRenewalBannerIfNecessary(walletState.showRenewBanner)
		if (!isRenewalBannerDisplayed) {
			displayEolBannerIfNecessary(walletState)
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
				if (signatureState.signatureErrorCode == ErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED) {
					infoBubbleColorId = R.color.blueish
					statusIconId = R.drawable.ic_invalid_grey
				} else {
					infoBubbleColorId = R.color.greyish
					statusIconId = R.drawable.ic_error_grey
				}
			}
			revocationState is CheckRevocationState.INVALID -> {
				infoBubbleColorId = R.color.greyish
				statusIconId = R.drawable.ic_error_grey
			}
			nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
				infoBubbleColorId = R.color.blueish
				statusIconId = R.drawable.ic_error_grey
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

		displayQrCodeRenewalBannerIfNecessary(state.showRenewBanner)
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

	private fun displayQrCodeRenewalBannerIfNecessary(showRenewBanner: String?): Boolean {
		val wasRecentlyRenewed = certificateHolder?.let { certificatesViewModel.wasCertificateRecentlyRenewed(it) } ?: false

		when {
			wasRecentlyRenewed -> {
				binding.certificatePageRenewalBanner.isVisible = true
				binding.certificatePageRenewalBannerDismiss.isVisible = true
				binding.certificatePageRenewalBannerTitle.setText(R.string.wallet_certificate_renewal_successful_bubble_title)
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.greenish)
				binding.certificatePageRenewalBanner.backgroundTintList = ColorStateList.valueOf(backgroundColor)

				binding.certificatePageRenewalBannerDismiss.setOnClickListener {
					certificateHolder?.let { certificate -> certificatesViewModel.dismissRecentlyRenewedBanner(certificate) }
					TransitionManager.beginDelayedTransition(binding.root)
					binding.certificatePageRenewalBanner.isVisible = false
				}

				return true
			}
			showRenewBanner != null -> {
				binding.certificatePageRenewalBanner.isVisible = true
				binding.certificatePageRenewalBannerDismiss.isVisible = false
				binding.certificatePageRenewalBannerTitle.setText(R.string.wallet_certificate_renewal_required_bubble_title)
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.redish)
				binding.certificatePageRenewalBanner.backgroundTintList = ColorStateList.valueOf(backgroundColor)

				return true
			}
			else -> {
				binding.certificatePageRenewalBanner.isVisible = false
				return false
			}
		}
	}

	private fun displayEolBannerIfNecessary(walletState: SuccessState.WalletSuccessState) {
		val isAlreadyDismissed = WalletSecureStorage.getInstance(requireContext()).getDismissedEolBanners().contains(qrCodeData)
		val eolBannerInfo = ConfigRepository.getCurrentConfig(requireContext())
			?.getEolBannerInfo(getString(R.string.language_key))
			?.get(walletState.eolBannerIdentifier)

		binding.certificatePageBanner.isVisible = eolBannerInfo != null && !isAlreadyDismissed

		eolBannerInfo?.let {
			val backgroundColor = try {
				Color.parseColor(it.homescreenHexColor)
			} catch (e: IllegalArgumentException) {
				ContextCompat.getColor(requireContext(), R.color.yellow)
			}

			binding.certificatePageBanner.backgroundTintList = ColorStateList.valueOf(backgroundColor)
			binding.certificatePageBannerTitle.text = it.homescreenTitle
			binding.certificatePageBannerDismiss.setOnClickListener {
				WalletSecureStorage.getInstance(requireContext()).addDismissedEolBanner(qrCodeData)
				TransitionManager.beginDelayedTransition(binding.root)
				binding.certificatePageBanner.isVisible = false
			}
		}
	}

}