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
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.config.EolBannerInfoModel
import ch.admin.bag.covidcertificate.common.config.WalletModeModel
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.extensions.getDrawableIdentifier
import ch.admin.bag.covidcertificate.common.extensions.overrideScreenBrightness
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.getInvalidErrorCode
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.common.views.animateBackgroundTintColor
import ch.admin.bag.covidcertificate.common.views.hideAnimated
import ch.admin.bag.covidcertificate.common.views.showAnimated
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.extensions.isChAusnahmeTest
import ch.admin.bag.covidcertificate.sdk.core.extensions.isNotFullyProtected
import ch.admin.bag.covidcertificate.sdk.core.extensions.isPositiveRatTest
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.*
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificateDetailBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemDetailModeBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemDetailModeRefreshBinding
import ch.admin.bag.covidcertificate.wallet.dialog.CertificateBannerInfoDialogFragment
import ch.admin.bag.covidcertificate.wallet.dialog.ModeInfoDialogFragment
import ch.admin.bag.covidcertificate.wallet.dialog.RefreshButtonInfoDialogFragment
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem
import ch.admin.bag.covidcertificate.wallet.light.CertificateLightConversionFragment
import ch.admin.bag.covidcertificate.wallet.pdf.export.PdfExportFragment
import ch.admin.bag.covidcertificate.wallet.pdf.export.PdfExportShareContract
import ch.admin.bag.covidcertificate.wallet.ratconversion.RatConversionFragment
import ch.admin.bag.covidcertificate.wallet.renewal.QrCodeRenewalFragment
import ch.admin.bag.covidcertificate.wallet.travel.ForeignValidityFragment
import ch.admin.bag.covidcertificate.wallet.util.*
import ch.admin.bag.covidcertificate.wallet.util.BitmapUtil.getHumanReadableName
import ch.admin.bag.covidcertificate.wallet.util.BitmapUtil.textAsBitmap
import ch.admin.bag.covidcertificate.wallet.vaccination.appointment.VaccinationAppointmentFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Integer.max
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class CertificateDetailFragment : Fragment() {

	companion object {
		private const val STATUS_HIDE_DELAY = 2000L
		private const val STATUS_LOAD_DELAY = 1000L

		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		private val ISSUER_SWITZERLAND = listOf("CH", "CH BAG")

		fun newInstance(certificateHolder: CertificateHolder): CertificateDetailFragment = CertificateDetailFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificateHolder)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesAndConfigViewModel>()

	private var _binding: FragmentCertificateDetailBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificateHolder: CertificateHolder

	private var hideDelayedJob: Job? = null
	private var eolBannerInfo: EolBannerInfoModel? = null

	private var isForceValidate = false
	private val pdfExportShareLauncher = registerForActivityResult(PdfExportShareContract()) { uri ->
		// Delete the cached pdf file when returning from the share intent
		uri?.let {
			requireContext().contentResolver.delete(it, null, null)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? CertificateHolder)
			?: throw IllegalStateException("Certificate detail fragment created without Certificate!")

		setFragmentResultListener(PdfExportFragment.REQUEST_KEY_PDF_EXPORT) { _, bundle ->
			bundle.getParcelable<Uri>(PdfExportFragment.RESULT_KEY_PDF_URI)?.let { uri ->
				pdfExportShareLauncher.launch(uri)
			}
		}
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
		setupDetailNote()
		setupConversionButtons()
		setupVaccinationAppointmentButton()
		setupQrCodeRenewalBanner()
		setupEolBanner()
		setupRatRecoveryConversionBanner()
		setupForeignValidityButton()

		binding.certificateDetailToolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		binding.certificateDetailButtonDelete.setOnClickListener {
			AlertDialog.Builder(view.context, R.style.CovidCertificate_AlertDialogStyle)
				.setTitle(R.string.delete_button)
				.setMessage(R.string.wallet_certificate_delete_confirm_text)
				.setPositiveButton(R.string.delete_button) { _, _ ->
					certificatesViewModel.removeCertificate(certificateHolder.qrCodeData)
					parentFragmentManager.popBackStack()
				}
				.setNegativeButton(R.string.cancel_button) { dialog, _ ->
					dialog.dismiss()
				}
				.setCancelable(true)
				.create()
				.show()
		}

		setupReverifyButtons()
	}

	override fun onResume() {
		super.onResume()
		requireActivity().window.overrideScreenBrightness(true)
	}

	override fun onPause() {
		super.onPause()
		requireActivity().window.overrideScreenBrightness(false)
	}

	private fun updateToolbarTitle() {
		val dccCert = certificateHolder.certificate as? DccCert

		val vaccinationEntry = dccCert?.vaccinations?.firstOrNull()
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
		val qrCodeBitmap = QrCode.renderToBitmap(certificateHolder.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificateDetailQrCode.setImageDrawable(qrCodeDrawable)
	}

	private fun setupCertificateDetails() {
		val recyclerView = binding.certificateDetailDataRecyclerView
		val adapter = CertificateDetailAdapter()
		recyclerView.adapter = adapter

		binding.certificateDetailName.text = certificateHolder.certificate.getPersonName().prettyName()
		binding.certificateDetailBirthdate.text = certificateHolder.certificate.getFormattedDateOfBirth()

		binding.certificateDetailInfo.setText(R.string.verifier_verify_success_info)

		val detailItems =
			CertificateDetailItemListBuilder(recyclerView.context, certificateHolder, isDetailScreen = true).buildAll()
		adapter.setItems(detailItems)
	}

	private fun setupStatusInfo() {
		certificatesViewModel.statefulWalletItems.observe(viewLifecycleOwner) { items ->
			items.filterIsInstance(StatefulWalletItem.VerifiedCertificate::class.java)
				.find { it.certificateHolder?.qrCodeData == certificateHolder.qrCodeData }?.let {
					val currentConfig = ConfigRepository.getCurrentConfig(requireContext())
					if (currentConfig?.refreshButtonDisabled != true) {
						binding.certificateDetailButtonReverify.showAnimated()
					}

					val isFeatureEnabled = currentConfig?.foreignRulesCheckEnabled == true
					val isNotInvalid = it.state is VerificationState.SUCCESS || it.state.isOnlyNationalRulesInvalid()
					val isValidOnlyInSwitzerland = it.state.isValidOnlyInSwitzerland()
					binding.certificateForeignValidityButton.isVisible = isFeatureEnabled && isNotInvalid && !isValidOnlyInSwitzerland

					updateStatusInfo(it.state)
				}
		}

		certificatesViewModel.startVerification(certificateHolder)
	}

	private fun setupDetailNote() {
		val dccCert = certificateHolder.certificate as? DccCert
		val stringId = when {
			dccCert?.tests?.firstOrNull()?.isChAusnahmeTest() == true -> R.string.wallet_certificate_detail_note_ausnahme
			dccCert?.tests?.firstOrNull()?.isPositiveRatTest() == true -> R.string.wallet_certificate_detail_note_positive_antigen
			else -> R.string.wallet_certificate_detail_note
		}

		binding.certificateDetailNote.text = HtmlCompat.fromHtml(getString(stringId), HtmlCompat.FROM_HTML_MODE_LEGACY)
		binding.certificateDetailNote.movementMethod = LinkMovementMethod.getInstance()
	}

	private fun setupReverifyButtons() {
		binding.certificateDetailButtonReverify.setOnClickListener {
			binding.certificateDetailButtonReverify.hideAnimated()
			binding.scrollview.smoothScrollTo(0, 0)
			isForceValidate = true
			hideDelayedJob?.cancel()
			certificatesViewModel.startVerification(
				certificateHolder,
				delayInMillis = STATUS_LOAD_DELAY,
				isForceVerification = true
			)
		}

		binding.certificateDetailButtonReverifyInfo.setOnClickListener {
			val config = ConfigRepository.getCurrentConfig(requireContext())
			val refreshButtonInfo = config?.getRefreshButtonInfo(getString(R.string.language_key))
			if (refreshButtonInfo != null) {
				RefreshButtonInfoDialogFragment.newInstance(refreshButtonInfo)
					.show(childFragmentManager, RefreshButtonInfoDialogFragment::class.java.canonicalName)
			}
		}

		certificatesViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			// Whenever the config live data changes, check if the refresh button has been disabled.
			// If yes, also check if the 2 week period for the info button has also passed
			if (config.refreshButtonDisabled == true) {
				val disabledTimestamp = ConfigSecureStorage.getInstance(requireContext()).getRefreshButtonDisabledTimestamp()
				val now = System.currentTimeMillis()
				val infoButtonPeriod = TimeUnit.DAYS.toMillis(14)

				binding.certificateDetailButtonReverify.isVisible = false
				binding.certificateDetailButtonReverifyInfo.isVisible = disabledTimestamp + infoButtonPeriod > now
			} else {
				binding.certificateDetailButtonReverify.isVisible = true
				binding.certificateDetailButtonReverifyInfo.isVisible = false
			}
		}

		setupReverifyButtonOffset()
	}

	private fun setupReverifyButtonOffset() {
		val certPos = intArrayOf(0, 0)
		val buttonPos = intArrayOf(0, 0)

		binding.root.post {
			if (isAdded) {
				reloadReverifyButtonOffset(certPos, buttonPos)
			}
		}
		binding.scrollview.setOnScrollChangeListener { _, _, _, _, _ -> reloadReverifyButtonOffset(certPos, buttonPos) }
	}

	private fun reloadReverifyButtonOffset(certPos: IntArray = intArrayOf(0, 0), buttonPos: IntArray = intArrayOf(0, 0)) {
		binding.certificateDetailQrCode.getLocationOnScreen(certPos)
		val certificateBottomY = certPos[1] + binding.certificateDetailQrCode.height

		val buttonTopY = when {
			binding.certificateDetailButtonReverify.isVisible -> {
				binding.certificateDetailButtonReverify.getLocationOnScreen(buttonPos)
				buttonPos[1] - binding.certificateDetailButtonReverify.translationY.toInt()
			}
			binding.certificateDetailButtonReverifyInfo.isVisible -> {
				binding.certificateDetailButtonReverifyInfo.getLocationOnScreen(buttonPos)
				buttonPos[1] - binding.certificateDetailButtonReverifyInfo.translationY.toInt()
			}
			else -> 0
		}

		val translationY = max(0, certificateBottomY - buttonTopY).toFloat()
		binding.certificateDetailButtonReverify.translationY = translationY
		binding.certificateDetailButtonReverifyInfo.translationY = translationY
	}

	private fun setupConversionButtons() {
		binding.certificateDetailConvertLightButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateLightConversionFragment.newInstance(certificateHolder))
				.addToBackStack(CertificateLightConversionFragment::class.java.canonicalName)
				.commit()
		}

		binding.certificateDetailConvertPdfButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, PdfExportFragment.newInstance(certificateHolder))
				.addToBackStack(PdfExportFragment::class.java.canonicalName)
				.commit()
		}
	}

	private fun setupVaccinationAppointmentButton() {
		val showVaccinationAppointmentButton =
			ConfigRepository.getCurrentConfig(requireContext())?.showVaccinationHintDetail ?: false
		binding.certificateDetailVaccinationAppointmentButton.isVisible = showVaccinationAppointmentButton

		binding.certificateDetailVaccinationAppointmentButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, VaccinationAppointmentFragment.newInstance())
				.addToBackStack(VaccinationAppointmentFragment::class.java.canonicalName)
				.commit()
		}
	}

	private fun setupQrCodeRenewalBanner() {
		binding.certificateDetailBanners.apply {
			qrCodeRenewalBanner.setOnClickListener {
				val fragment = QrCodeRenewalFragment.newInstance(certificateHolder)
				parentFragmentManager.beginTransaction()
					.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
					.replace(R.id.fragment_container, fragment)
					.addToBackStack(QrCodeRenewalFragment::class.java.canonicalName)
					.commit()
			}
		}
	}

	private fun setupEolBanner() {
		binding.certificateDetailBanners.certificateDetailBanner.setOnClickListener {
			eolBannerInfo?.let {
				CertificateBannerInfoDialogFragment.newInstance(it)
					.show(childFragmentManager, CertificateBannerInfoDialogFragment::class.java.canonicalName)
			}
		}
	}

	private fun setupRatRecoveryConversionBanner() {
		binding.certificateDetailBanners.ratConversionBanner.setOnClickListener {
			val fragment = RatConversionFragment.newInstance(certificateHolder)
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, fragment)
				.addToBackStack(RatConversionFragment::class.java.canonicalName)
				.commit()
		}
	}

	private fun setupForeignValidityButton() {
		binding.certificateForeignValidityButton.setOnClickListener {
			val fragment = ForeignValidityFragment.newInstance(certificateHolder)
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, fragment)
				.addToBackStack(ForeignValidityFragment::class.java.canonicalName)
				.commit()
		}
	}

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return

		changeAlpha(state)
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
		updateConversionButtons(isLightCertificateEnabled = false, isPdfExportEnabled = false)

		val info = SpannableString(context.getString(R.string.wallet_certificate_verifying))
		if (isForceValidate) {
			showStatusInfoAndDescription(null, info, 0)
			showForceValidation(R.color.grey, 0, 0, info, emptyList())
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
		val walletState = state.successState as SuccessState.WalletSuccessState

		showValidityDate(walletState.validityRange?.validUntil, certificateHolder.certType, state)

		setInfoBubbleBackgrounds(R.color.blueish, R.color.greenish)

		// Certificate Light and PDF export is enabled for a valid certificate that was issued in Switzerland
		val isIssuedInSwitzerland = ISSUER_SWITZERLAND.contains(certificateHolder.issuer)
		updateConversionButtons(isLightCertificateEnabled = isIssuedInSwitzerland, isPdfExportEnabled = isIssuedInSwitzerland)

		val statusInfo: SpannableString
		val iconId: Int
		val showRedBorder: Boolean
		if (walletState.isValidOnlyInSwitzerland) {
			statusInfo = SpannableString(context.getString(R.string.wallet_only_valid_in_switzerland))
			iconId = R.drawable.ic_flag_ch
			showRedBorder = true
		} else {
			statusInfo = SpannableString(context.getString(R.string.verifier_verify_success_info))
			iconId = R.drawable.ic_info_blue
			showRedBorder = false
		}
		val forceValidationInfo = context.getString(R.string.wallet_certificate_verify_success).makeBold()
		if (isForceValidate) {
			showStatusInfoAndDescription(null, forceValidationInfo, R.drawable.ic_check_green)
			showForceValidation(
				R.color.green,
				R.drawable.ic_check_green,
				R.drawable.ic_check_large,
				forceValidationInfo,
				walletState.modeValidity
			)
			readjustStatusDelayed(R.color.blueish, iconId, statusInfo, showRedBorder)
		} else {
			showStatusInfoAndDescription(null, statusInfo, iconId, showRedBorder)
		}

		showModes(walletState.modeValidity)
		setupModesButton(walletState.modeValidity)

		displayQrCodeRenewalBannerIfNecessary(walletState.showRenewBanner)

		eolBannerInfo = ConfigRepository.getCurrentConfig(requireContext())
			?.getEolBannerInfo(getString(R.string.language_key))
			?.get(walletState.eolBannerIdentifier)

		binding.certificateDetailBanners.certificateDetailBanner.isVisible = eolBannerInfo != null

		eolBannerInfo?.let { info ->
			val backgroundColor = try {
				Color.parseColor(info.homescreenHexColor)
			} catch (e: IllegalArgumentException) {
				ContextCompat.getColor(requireContext(), R.color.yellow)
			}

			binding.certificateDetailBanners.apply {
				certificateDetailBanner.backgroundTintList = ColorStateList.valueOf(backgroundColor)
				certificateDetailBannerTitle.text = info.detailTitle
				certificateDetailBannerText.text = info.detailText
				certificateDetailBannerMoreInfo.text = info.detailMoreInfo
			}
		}

		// Show the RAT recovery certificate conversion banner if the feature flag is enabled and the certificate has the correct type
		val showRatRecoveryConversionBanner = ConfigRepository.getCurrentConfig(requireContext())?.showRatConversionForm == true
		val isPositiveRatTest = (certificateHolder.certificate as? DccCert)?.tests?.firstOrNull()?.isPositiveRatTest() ?: false
		binding.certificateDetailBanners.ratConversionBanner.isVisible = showRatRecoveryConversionBanner && isPositiveRatTest
	}

	private fun setupModesButton(modeValidities: List<ModeValidity>) {
		val arrayList = arrayListOf<ModeValidity>()
		arrayList.addAll(modeValidities)
		binding.certificateDetailInfoModes.certificateDetailInfoModesList.setOnClickListener {
			ModeInfoDialogFragment.newInstance(arrayList)
				.show(childFragmentManager, ModeInfoDialogFragment::class.java.canonicalName)
		}
	}

	private fun showModes(modeValidities: List<ModeValidity>) {
		if (modeValidities.size <= 1) return
		binding.certificateDetailInfoModes.certificateDetailInfoModesList.removeAllViews()
		val configLiveData: ConfigModel? = certificatesViewModel.configLiveData.value
		val checkedModes = configLiveData?.getCheckModes(getString(R.string.language_key))

		for (modeValidity in modeValidities) {
			val modeValidityState = modeValidity.modeValidityState
			if (modeValidityState.isLight() || modeValidityState.isUnknown()) {
				continue
			}

			val itemBinding = ItemDetailModeBinding.inflate(
				layoutInflater,
				binding.certificateDetailInfoModes.certificateDetailInfoModesList,
				true
			)
			val imageView = itemBinding.root
			val walletModeModel: WalletModeModel? = checkedModes?.get(modeValidity.mode)
			val resOk = requireContext().getDrawableIdentifier(walletModeModel?.ok?.iconAndroid ?: "")
			val resNotOk = requireContext().getDrawableIdentifier(walletModeModel?.notOk?.iconAndroid ?: "")

			if (modeValidity.modeValidityState == ModeValidityState.SUCCESS) {
				if (resOk != 0) {
					imageView.setImageResource(resOk)
				} else {
					val bitmap =
						textAsBitmap(
							requireContext(),
							getHumanReadableName(modeValidity.mode),
							resources.getDimensionPixelSize(R.dimen.text_size_small),
							ContextCompat.getColor(requireContext(), R.color.blue),
							ContextCompat.getColor(requireContext(), android.R.color.white)
						)
					imageView.setImageBitmap(bitmap)
				}
				imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue))
			} else if (modeValidityState.isPartiallyValid() || modeValidityState.isInvalid()) {
				val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.grey))
				if (resNotOk != 0) {
					imageView.setImageResource(resNotOk)
					imageView.imageTintList = colorStateList
				} else {
					val bitmap =
						textAsBitmap(
							requireContext(),
							getHumanReadableName(modeValidity.mode),
							resources.getDimensionPixelSize(R.dimen.text_size_small),
							ContextCompat.getColor(requireContext(), R.color.grey),
							ContextCompat.getColor(requireContext(), android.R.color.white),
							isNotOK = true
						)
					imageView.setImageBitmap(bitmap)
				}
			}
		}
	}

	private fun showModesForRefresh(modeValidities: List<ModeValidity>) {
		if (modeValidities.size <= 1) return
		val configLiveData: ConfigModel? = certificatesViewModel.configLiveData.value
		val checkedModes = configLiveData?.getCheckModes(getString(R.string.language_key))

		for (modeValidity in modeValidities) {
			val modeValidityState = modeValidity.modeValidityState
			if (modeValidityState.isLight() || modeValidityState.isUnknown()) {
				continue
			}

			val itemBinding =
				ItemDetailModeRefreshBinding.inflate(layoutInflater, binding.certificateDetailRefreshModeValidity, true)
			val imageView = itemBinding.root
			val walletModeModel: WalletModeModel? = checkedModes?.get(modeValidity.mode)
			val resOk = requireContext().getDrawableIdentifier(walletModeModel?.ok?.iconAndroid ?: "")
			val resNotOk = requireContext().getDrawableIdentifier(walletModeModel?.notOk?.iconAndroid ?: "")

			if (modeValidityState.isValid()) {
				if (resOk != 0) {
					imageView.setImageResource(resOk)
				} else {
					val bitmap =
						textAsBitmap(
							requireContext(),
							getHumanReadableName(modeValidity.mode),
							resources.getDimensionPixelSize(R.dimen.text_size_small),
							ContextCompat.getColor(requireContext(), R.color.white),
							ContextCompat.getColor(requireContext(), R.color.green),
						)
					imageView.setImageBitmap(bitmap)
				}
				imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
			} else if (modeValidityState.isPartiallyValid() || modeValidityState.isInvalid()) {
				val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
				if (resNotOk != 0) {
					imageView.setImageResource(resNotOk)
					imageView.imageTintList = colorStateList
				} else {
					val bitmap =
						textAsBitmap(
							requireContext(),
							getHumanReadableName(modeValidity.mode),
							resources.getDimensionPixelSize(R.dimen.text_size_small),
							ContextCompat.getColor(requireContext(), R.color.black),
							ContextCompat.getColor(requireContext(), R.color.green),
							isNotOK = true
						)
					imageView.setImageBitmap(bitmap)
				}

			}
		}
	}

	private fun displayInvalidState(state: VerificationState.INVALID) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = false

		binding.certificateDetailInfoValidityGroup.isVisible = state.signatureState == CheckSignatureState.SUCCESS
		showValidityDate(state.validityRange?.validUntil, certificateHolder.certType, state)

		// Certificate Light is disabled for invalid certificates, PDF export is enabled if the signature is valid and the certificate was issued in Switzerland
		val isSignatureValid = state.signatureState is CheckSignatureState.SUCCESS
		val isIssuedInSwitzerland = ISSUER_SWITZERLAND.contains(certificateHolder.issuer)
		updateConversionButtons(isLightCertificateEnabled = false, isPdfExportEnabled = isSignatureValid && isIssuedInSwitzerland)

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
		val signatureState = state.signatureState
		val revocationState = state.revocationState
		val nationalRulesState = state.nationalRulesState

		when {
			signatureState is CheckSignatureState.INVALID -> {
				icon = R.drawable.ic_error_grey
				forceValidationIcon = R.drawable.ic_error
			}
			revocationState is CheckRevocationState.INVALID -> {
				icon = R.drawable.ic_error_grey
				forceValidationIcon = R.drawable.ic_error
			}
			nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
				icon = R.drawable.ic_invalid_grey
				forceValidationIcon = R.drawable.ic_invalid_red
			}
			nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> {
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
			showForceValidation(R.color.red, forceValidationIcon, R.drawable.ic_error_large, info, emptyList())
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

		displayQrCodeRenewalBannerIfNecessary(state.showRenewBanner)
	}

	private fun displayErrorState(state: VerificationState.ERROR) {
		val context = context ?: return
		showLoadingIndicator(false)
		binding.certificateDetailInfoDescriptionGroup.isVisible = true
		binding.certificateDetailInfoValidityGroup.isVisible = false
		setInfoBubbleBackgrounds(R.color.greyish, R.color.orangeish)
		updateConversionButtons(isLightCertificateEnabled = false, isPdfExportEnabled = false)

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
		} else if (state.isTimeInconsistency()) {
			info = context.getString(R.string.wallet_time_inconsistency_error_title).makeBold()
			forceValidationInfo = context.getString(R.string.wallet_time_inconsistency_error_title).makeBold()
			description = SpannableString(context.getString(R.string.wallet_time_inconsistency_error_text))
			icon = R.drawable.ic_timeerror
			forceValidationIcon = R.drawable.ic_timeerror_orange
			forceValidationIconLarge = R.drawable.ic_timeerror_large
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
			showForceValidation(R.color.orange, forceValidationIcon, forceValidationIconLarge, forceValidationInfo, emptyList())
			readjustStatusDelayed(R.color.greyish, icon, info)
		} else {
			showStatusInfoAndDescription(description, info, icon)
		}

		binding.certificateDetailErrorCode.apply {
			isVisible = true
			text = state.error.code
		}
	}

	private fun updateConversionButtons(isLightCertificateEnabled: Boolean, isPdfExportEnabled: Boolean) {
		val currentConfig = ConfigRepository.getCurrentConfig(requireContext())

		if (currentConfig?.lightCertificateActive == true) {
			binding.certificateDetailConvertLightButton.isVisible = true
			binding.certificateDetailConvertLightButton.isEnabled = isLightCertificateEnabled
			binding.certificateDetailConvertLightArrow.isVisible = isLightCertificateEnabled
		} else {
			binding.certificateDetailConvertLightButton.isVisible = false
		}

		if (currentConfig?.pdfGenerationActive == true) {
			binding.certificateDetailConvertPdfButton.isVisible = true
			binding.certificateDetailConvertPdfButton.isEnabled = isPdfExportEnabled
			binding.certificateDetailConvertPdfArrow.isVisible = isPdfExportEnabled
		} else {
			binding.certificateDetailConvertPdfButton.isVisible = false
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
	private fun changeAlpha(state: VerificationState) {
		binding.certificateDetailQrCode.alpha = state.getInvalidQrCodeAlpha(certificateHolder.certType == CertType.TEST)
		val contentAlpha = state.getInvalidContentAlpha()
		binding.certificateDetailInfoValidityDateDisclaimer.alpha = contentAlpha
		binding.certificateDetailInfoValidityDateGroup.alpha = contentAlpha
		binding.certificateDetailDataRecyclerView.alpha = contentAlpha
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
		modeValidities: List<ModeValidity>
	) {
		binding.certificateDetailRefreshModeValidity.removeAllViews()
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
		showModesForRefresh(modeValidities)
	}

	/**
	 * Display the verification status info and description
	 */
	private fun showStatusInfoAndDescription(
		description: SpannableString?,
		info: SpannableString?,
		@DrawableRes iconId: Int,
		showRedBorder: Boolean = false
	) {
		binding.certificateDetailInfoDescription.text = description
		binding.certificateDetailInfo.text = info
		binding.certificateDetailStatusIcon.setImageResource(iconId)
		binding.certificateDetailInfoRedBorder.visibility = if (showRedBorder) View.VISIBLE else View.GONE
	}

	/**
	 * Reset the view after a delay from the force validation verification state to the regular verification state
	 */
	private fun readjustStatusDelayed(
		@ColorRes infoBubbleColorId: Int,
		@DrawableRes statusIconId: Int,
		info: SpannableString?,
		showRedBorder: Boolean = false
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
			binding.certificateDetailInfoRedBorder.visibility = if (showRedBorder) View.VISIBLE else View.GONE

			binding.certificateDetailInfoVerificationStatus.hideAnimated()
			binding.certificateDetailInfoValidityGroup.animateBackgroundTintColor(
				ContextCompat.getColor(context, infoBubbleColorId)
			)

			binding.certificateDetailStatusIcon.setImageResource(statusIconId)

			binding.certificateDetailButtonReverify.showAnimated()
			isForceValidate = false
		}
	}

	private fun displayQrCodeRenewalBannerIfNecessary(showRenewBanner: String?) {
		val wasRecentlyRenewed = certificatesViewModel.wasCertificateRecentlyRenewed(certificateHolder)

		when {
			wasRecentlyRenewed -> {
				binding.certificateDetailBanners.apply {
					qrCodeRenewalBanner.isVisible = true
					qrCodeRenewalBannerDismiss.isVisible = true

					qrCodeRenewalBannerTitle.setText(R.string.wallet_certificate_renewal_successful_bubble_title)
					qrCodeRenewalBannerText.setText(R.string.wallet_certificate_renewal_successful_bubble_text)
					qrCodeRenewalBannerMoreInfo.setText(R.string.wallet_certificate_renewal_successful_bubble_button)
					val backgroundColor = ContextCompat.getColor(requireContext(), R.color.greenish)
					qrCodeRenewalBanner.animateBackgroundTintColor(backgroundColor)

					qrCodeRenewalBannerDismiss.setOnClickListener {
						certificatesViewModel.dismissRecentlyRenewedBanner(certificateHolder)
						TransitionManager.beginDelayedTransition(binding.root)
						qrCodeRenewalBanner.isVisible = false
					}
				}
			}
			showRenewBanner != null -> {
				binding.certificateDetailBanners.apply {
					qrCodeRenewalBanner.isVisible = true
					qrCodeRenewalBannerDismiss.isVisible = false

					qrCodeRenewalBannerTitle.setText(R.string.wallet_certificate_renewal_required_bubble_title)
					qrCodeRenewalBannerText.setText(R.string.wallet_certificate_renewal_required_bubble_text)
					qrCodeRenewalBannerMoreInfo.setText(R.string.wallet_certificate_renewal_required_bubble_button)
					val backgroundColor = ContextCompat.getColor(requireContext(), R.color.redish)
					qrCodeRenewalBanner.animateBackgroundTintColor(backgroundColor)
				}
			}
			else -> {
				binding.certificateDetailBanners.qrCodeRenewalBanner.isVisible = false
			}
		}
	}

}