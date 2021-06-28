/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.detail

import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ch.admin.bag.covidcertificate.common.util.getInvalidErrorCode
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.common.util.setSecureFlagToBlockScreenshots
import ch.admin.bag.covidcertificate.common.views.animateBackgroundTintColor
import ch.admin.bag.covidcertificate.common.views.hideAnimated
import ch.admin.bag.covidcertificate.common.views.showAnimated
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.euhealthcert.VaccinationEntry
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.utils.*
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificateDetailBinding
import ch.admin.bag.covidcertificate.wallet.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CertificateDetailFragment : Fragment() {

	companion object {
		private const val STATUS_HIDE_DELAY = 2000L
		private const val STATUS_LOAD_DELAY = 1000L

		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: DccHolder): CertificateDetailFragment = CertificateDetailFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificateDetailBinding? = null
	private val binding get() = _binding!!

	private lateinit var dccHolder: DccHolder

	private var hideDelayedJob: Job? = null

	private var isForceValidate = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dccHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? DccHolder)
			?: throw IllegalStateException("Certificate detail fragment created without Certificate!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificateDetailBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		displayQrCode()
		updateToolbarTitle()
		setupCertificateDetails()
		setupStatusInfo()

		binding.certificateDetailToolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		binding.certificateDetailButtonDelete.setOnClickListener {
			AlertDialog.Builder(view.context, R.style.CovidCertificate_AlertDialogStyle)
				.setTitle(R.string.delete_button)
				.setMessage(R.string.wallet_certificate_delete_confirm_text)
				.setPositiveButton(R.string.delete_button) { _, _ ->
					certificatesViewModel.removeCertificate(dccHolder.qrCodeData)
					parentFragmentManager.popBackStack()
				}
				.setNegativeButton(R.string.cancel_button) { dialog, _ ->
					dialog.dismiss()
				}
				.setCancelable(true)
				.create()
				.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
				.show()
		}

		binding.certificateDetailButtonReverify.setOnClickListener {
			binding.certificateDetailButtonReverify.hideAnimated()
			binding.scrollview.smoothScrollTo(0, 0)
			isForceValidate = true
			hideDelayedJob?.cancel()
			certificatesViewModel.startVerification(dccHolder, delayInMillis = STATUS_LOAD_DELAY, isForceVerification = true)
		}
	}

	private fun updateToolbarTitle() {
		val vaccinationEntry: VaccinationEntry? = dccHolder.euDGC.vaccinations?.firstOrNull()
		if (vaccinationEntry?.isNotFullyProtected() == true) {
			binding.certificateDetailToolbar.setTitle(R.string.wallet_certificate_evidence_title)
		} else {
			binding.certificateDetailToolbar.setTitle(R.string.covid_certificate_title)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun displayQrCode() {
		val qrCodeBitmap = QrCode.renderToBitmap(dccHolder.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificateDetailQrCode.setImageDrawable(qrCodeDrawable)
	}

	private fun setupCertificateDetails() {
		val recyclerView = binding.certificateDetailDataRecyclerView
		val adapter = CertificateDetailAdapter()
		recyclerView.adapter = adapter

		val name = "${dccHolder.euDGC.person.familyName} ${dccHolder.euDGC.person.givenName}"
		binding.certificateDetailName.text = name
		val dateOfBirth = dccHolder.euDGC.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificateDetailBirthdate.text = dateOfBirth

		binding.certificateDetailInfo.setText(R.string.verifier_verify_success_info)

		val detailItems = CertificateDetailItemListBuilder(recyclerView.context, dccHolder).buildAll()
		adapter.setItems(detailItems)
	}

	private fun setupStatusInfo() {
		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { certificates ->
			certificates.find { it.dccHolder?.qrCodeData == dccHolder.qrCodeData }?.let {
				binding.certificateDetailButtonReverify.showAnimated()
				updateStatusInfo(it.state)
			}
		}

		certificatesViewModel.startVerification(dccHolder)
	}

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return

		changeAlpha(state.getQrAlpha())
		setCertificateDetailTextColor(state.getNameDobColor())

		when (state) {
			is VerificationState.LOADING -> displayLoadingState()
			is VerificationState.SUCCESS -> displaySuccessState(state)
			is VerificationState.INVALID -> displayInvalidState(state)
			is VerificationState.ERROR -> displayErrorState(state)
		}
	}

	private fun displayLoadingState() {
		val context = context ?: return
		showLoadingIndicator(true)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoValidityGroup.isVisible = false
		binding.certificateDetailErrorCode.isVisible = false
		setInfoBubbleBackgrounds(R.color.greyish, R.color.greyish)

		val info = SpannableString(context.getString(R.string.wallet_certificate_verifying))
		if (isForceValidate) {
			showStatusInfoAndDescription(null, info, 0)
			showForceValidation(R.color.grey, 0, 0, info)
		} else {
			showStatusInfoAndDescription(null, info, 0)
		}
	}

	private fun displaySuccessState(state: VerificationState.SUCCESS) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoValidityGroup.isVisible = true
		binding.certificateDetailErrorCode.isVisible = false
		showValidityDate(state.validityRange.validUntil, dccHolder.certType, state)
		setInfoBubbleBackgrounds(R.color.blueish, R.color.greenish)

		val info = SpannableString(context.getString(R.string.verifier_verify_success_info))
		val forceValidationInfo = context.getString(R.string.wallet_certificate_verify_success).makeBold()
		if (isForceValidate) {
			showStatusInfoAndDescription(null, forceValidationInfo, R.drawable.ic_check_green)
			showForceValidation(R.color.green, R.drawable.ic_check_green, R.drawable.ic_check_large, forceValidationInfo)
			readjustStatusDelayed(R.color.blueish, R.drawable.ic_info_blue, info)
		} else {
			showStatusInfoAndDescription(null, info, R.drawable.ic_info_blue)
		}
	}

	private fun displayInvalidState(state: VerificationState.INVALID) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false
		binding.certificateDetailInfoValidityGroup.isVisible = true
		showValidityDate(state.validityRange?.validUntil, dccHolder.certType, state)

		val info = state.getValidationStatusString(context)
		val infoBubbleColorId = when {
			state.signatureState is CheckSignatureState.INVALID -> R.color.greyish
			state.revocationState is CheckRevocationState.INVALID -> R.color.greyish
			state.nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> R.color.blueish
			state.nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> R.color.blueish
			state.nationalRulesState is CheckNationalRulesState.INVALID -> R.color.greyish
			else -> R.color.greyish
		}

		setInfoBubbleBackgrounds(infoBubbleColorId, R.color.redish)

		val icon: Int
		val forceValidationIcon: Int
		when (state.nationalRulesState) {
			is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
				icon = R.drawable.ic_invalid_grey
				forceValidationIcon = R.drawable.ic_invalid_red
			}
			is CheckNationalRulesState.NOT_YET_VALID -> {
				icon = R.drawable.ic_timelapse
				forceValidationIcon = R.drawable.ic_timelapse_red
			}
			else -> {
				icon = R.drawable.ic_error_grey
				forceValidationIcon = R.drawable.ic_error
			}
		}

		if (isForceValidate) {
			showStatusInfoAndDescription(null, info, forceValidationIcon)
			showForceValidation(R.color.red, forceValidationIcon, R.drawable.ic_error_large, info)
			readjustStatusDelayed(infoBubbleColorId, icon, info)
		} else {
			showStatusInfoAndDescription(null, info, icon)
		}

		binding.certificateDetailErrorCode.apply {
			val errorCode = state.getInvalidErrorCode(showNationalErrors = true)
			if (errorCode.isNotEmpty()) {
				isVisible = true
				text = errorCode
			} else {
				isVisible = false
			}
		}
	}

	private fun displayErrorState(state: VerificationState.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = true
		binding.certificateDetailInfoValidityGroup.isVisible = false
		setInfoBubbleBackgrounds(R.color.greyish, R.color.orangeish)

		val info: SpannableString
		val forceValidationInfo: SpannableString
		val description: SpannableString
		val icon: Int
		val forceValidationIcon: Int
		val forceValidationIconLarge: Int
		if (state.isOfflineMode()) {
			info = context.getString(R.string.wallet_homescreen_offline).makeBold()
			forceValidationInfo = context.getString(R.string.wallet_detail_offline_retry_title).makeBold()
			description = SpannableString(context.getString(R.string.wallet_offline_description))
			icon = R.drawable.ic_offline
			forceValidationIcon = R.drawable.ic_offline_orange
			forceValidationIconLarge = R.drawable.ic_offline_large
		} else {
			info = SpannableString(context.getString(R.string.wallet_homescreen_network_error))
			forceValidationInfo = context.getString(R.string.wallet_detail_network_error_title).makeBold()
			description = SpannableString(context.getString(R.string.wallet_detail_network_error_text))
			icon = R.drawable.ic_process_error_grey
			forceValidationIcon = R.drawable.ic_process_error
			forceValidationIconLarge = R.drawable.ic_process_error_large
		}

		if (isForceValidate) {
			showStatusInfoAndDescription(description, forceValidationInfo, icon)
			showForceValidation(R.color.orange, forceValidationIcon, forceValidationIconLarge, forceValidationInfo)
			readjustStatusDelayed(R.color.greyish, icon, info)
		} else {
			showStatusInfoAndDescription(description, info, icon)
		}

		binding.certificateDetailErrorCode.apply {
			isVisible = true
			text = state.error.code
		}
	}

	/**
	 * Show or hide the loading indicators and status icons in the QR code and the info bubble
	 */
	private fun showLoadingIndicator(isLoading: Boolean) {
		binding.certificateDetailStatusLoading.isVisible = isLoading
		binding.certificateDetailStatusIcon.isVisible = !isLoading

		binding.certificateDetailQrCodeLoading.isVisible = isLoading
		binding.certificateDetailQrCodeStatusIcon.isVisible = !isLoading
	}

	/**
	 * Change the alpha value of the QR code, validity date and certificate content
	 */
	private fun changeAlpha(alpha: Float) {
		binding.certificateDetailQrCode.alpha = alpha
		binding.certificateDetailInfoValidityDateDisclaimer.alpha = alpha
		binding.certificateDetailInfoValidityDateGroup.alpha = alpha
		binding.certificateDetailDataRecyclerView.alpha = alpha
	}

	/**
	 * Display the formatted validity date of the vaccine or test
	 */
	private fun showValidityDate(validUntil: LocalDateTime?, certificateType: CertType?, verificationState: VerificationState) {
		val formatter = when (certificateType) {
			null -> null
			CertType.TEST -> DEFAULT_DISPLAY_DATE_TIME_FORMATTER
			else -> DEFAULT_DISPLAY_DATE_FORMATTER
		}
		val isCertificateRevoked = verificationState is VerificationState.INVALID &&
				verificationState.revocationState is CheckRevocationState.INVALID
		val formattedDate = if (isCertificateRevoked || validUntil == null) {
			"-"
		} else {
			formatter?.format(validUntil)
		}
		binding.certificateDetailInfoValidityDate.text = formattedDate
	}

	/**
	 * Set the text color of the certificate details (person name and date of birth)
	 */
	private fun setCertificateDetailTextColor(@ColorRes colorId: Int) {
		val textColor = ContextCompat.getColor(requireContext(), colorId)
		binding.certificateDetailName.setTextColor(textColor)
		binding.certificateDetailBirthdate.setTextColor(textColor)
	}

	/**
	 * Set the info bubble backgrounds, depending if a force validation is running or not
	 */
	private fun setInfoBubbleBackgrounds(@ColorRes infoBubbleColorId: Int, @ColorRes infoBubbleValidationColorId: Int) {
		val infoBubbleColor = ContextCompat.getColor(requireContext(), infoBubbleColorId)
		val infoBubbleValidationColor = ContextCompat.getColor(requireContext(), infoBubbleValidationColorId)

		if (isForceValidate) {
			binding.certificateDetailInfo.animateBackgroundTintColor(infoBubbleColor)
			binding.certificateDetailInfoVerificationStatus.animateBackgroundTintColor(infoBubbleValidationColor)
			binding.certificateDetailInfoDescriptionGroup.animateBackgroundTintColor(infoBubbleValidationColor)
			binding.certificateDetailInfoValidityGroup.animateBackgroundTintColor(infoBubbleValidationColor)
		} else {
			val infoBubbleColorTintList = ColorStateList.valueOf(infoBubbleColor)
			binding.certificateDetailInfo.backgroundTintList = infoBubbleColorTintList
			binding.certificateDetailInfoVerificationStatus.backgroundTintList = ColorStateList.valueOf(infoBubbleValidationColor)
			binding.certificateDetailInfoDescriptionGroup.backgroundTintList = infoBubbleColorTintList
			binding.certificateDetailInfoValidityGroup.backgroundTintList = infoBubbleColorTintList
		}
	}

	/**
	 * Display the correct QR code background, icons and text when a force validation is running
	 */
	private fun showForceValidation(
		@ColorRes solidValidationColorId: Int,
		@DrawableRes validationIconId: Int,
		@DrawableRes validationIconLargeId: Int,
		info: SpannableString?,
	) {
		binding.certificateDetailQrCodeColor.animateBackgroundTintColor(
			ContextCompat.getColor(
				requireContext(),
				solidValidationColorId
			)
		)
		binding.certificateDetailQrCodeStatusIcon.setImageResource(validationIconLargeId)
		binding.certificateDetailStatusIcon.setImageResource(validationIconId)

		if (!binding.certificateDetailQrCodeStatusGroup.isVisible) binding.certificateDetailQrCodeStatusGroup.showAnimated()

		binding.certificateDetailInfoVerificationStatus.apply {
			text = info
			if (!isVisible) showAnimated()
		}
	}

	/**
	 * Display the verification status info and description
	 */
	private fun showStatusInfoAndDescription(description: SpannableString?, info: SpannableString?, @DrawableRes iconId: Int) {
		binding.certificateDetailInfoDescription.text = description
		binding.certificateDetailInfo.text = info
		binding.certificateDetailStatusIcon.setImageResource(iconId)
	}

	/**
	 * Reset the view after a delay from the force validation verification state to the regular verification state
	 */
	private fun readjustStatusDelayed(
		@ColorRes infoBubbleColorId: Int,
		@DrawableRes statusIconId: Int,
		info: SpannableString?,
	) {
		hideDelayedJob?.cancel()
		hideDelayedJob = viewLifecycleOwner.lifecycleScope.launch {
			delay(STATUS_HIDE_DELAY)
			if (!isActive || !isVisible) return@launch

			val context = binding.root.context

			binding.certificateDetailQrCodeStatusGroup.hideAnimated()
			binding.certificateDetailQrCodeColor.animateBackgroundTintColor(
				ContextCompat.getColor(context, android.R.color.transparent)
			)

			binding.certificateDetailInfo.text = info
			binding.certificateDetailInfoDescriptionGroup.animateBackgroundTintColor(
				ContextCompat.getColor(context, infoBubbleColorId)
			)

			binding.certificateDetailInfoVerificationStatus.hideAnimated()
			binding.certificateDetailInfoValidityGroup.animateBackgroundTintColor(
				ContextCompat.getColor(context, infoBubbleColorId)
			)

			binding.certificateDetailStatusIcon.setImageResource(statusIconId)

			binding.certificateDetailButtonReverify.showAnimated()
			isForceValidate = false
		}
	}

}