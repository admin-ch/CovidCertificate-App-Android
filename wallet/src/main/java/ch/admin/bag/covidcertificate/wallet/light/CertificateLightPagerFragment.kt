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

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.common.views.animateBackgroundTintColor
import ch.admin.bag.covidcertificate.common.views.setCutOutCardBackground
import ch.admin.bag.covidcertificate.sdk.core.extensions.fromBase64
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificateLightPagerBinding
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem
import ch.admin.bag.covidcertificate.wallet.util.*
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

class CertificateLightPagerFragment : Fragment(R.layout.fragment_certificate_light_pager) {

	companion object {
		private const val ARG_QR_CODE_IMAGE = "ARG_QR_CODE_IMAGE"
		private const val ARG_CERTIFICATE_HOLDER = "ARG_CERTIFICATE_HOLDER"

		fun newInstance(qrCodeImage: String, certificateHolder: CertificateHolder) =
			CertificateLightPagerFragment().apply {
				arguments = bundleOf(
					ARG_QR_CODE_IMAGE to qrCodeImage,
					ARG_CERTIFICATE_HOLDER to certificateHolder
				)
			}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val certificateLightViewModel by viewModels<CertificateLightViewModel>()

	private var _binding: FragmentCertificateLightPagerBinding? = null
	private val binding get() = _binding!!

	private lateinit var qrCodeImage: String
	private lateinit var certificateHolder: CertificateHolder

	private var validityTimer: Timer? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		qrCodeImage = arguments?.getString(ARG_QR_CODE_IMAGE)
			?: throw IllegalArgumentException("CertificateLightPagerFragment called without QR code image")
		certificateHolder = arguments?.getSerializable(ARG_CERTIFICATE_HOLDER) as? CertificateHolder
			?: throw IllegalArgumentException("CertificateLightPagerFragment called without certificate holder")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificateLightPagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.certificatePageCard.setCutOutCardBackground()

		displayQrCode()
		displayCertificateDetails()
		displayValidity()

		setupStatusInfo()

		binding.certificatePageCard.setOnClickListener {
			certificatesViewModel.onCertificateLightClicked(qrCodeImage, certificateHolder)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
		validityTimer?.cancel()
		validityTimer = null
	}

	private fun displayQrCode() {
		val decoded = qrCodeImage.fromBase64()
		val qrCodeBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
		val qrCodeDrawable = BitmapDrawable(resources, QrCode.convertBitmapToBw(qrCodeBitmap))
		binding.certificatePageQrCode.setImageDrawable(qrCodeDrawable)
	}

	private fun displayCertificateDetails() {
		val name = certificateHolder.certificate.getPersonName().let { "${it.familyName} ${it.givenName}" }
		binding.certificatePageName.text = name
		binding.certificatePageBirthdate.text = certificateHolder.certificate.getFormattedDateOfBirth()
	}

	@SuppressLint("SetTextI18n")
	private fun displayValidity() {
		val expirationTime = certificateHolder.expirationTime ?: return

		validityTimer?.cancel()
		validityTimer = fixedRateTimer(period = TimeUnit.SECONDS.toMillis(1L)) {
			val now = Instant.now()
			val remainingTimeInSeconds = expirationTime.epochSecond - now.epochSecond
			val hours = remainingTimeInSeconds / 3600
			val minutes = remainingTimeInSeconds / 60 % 60
			val seconds = remainingTimeInSeconds % 60

			view?.post {
				if (isAdded) {
					binding.certificateLightPageValidity.text = "%02d:%02d:%02d".format(hours, minutes, seconds)
				}
			}

			if (remainingTimeInSeconds <= 0) {
				validityTimer?.cancel()
				validityTimer = null
				deleteCertificateLightAndReloadWalletData()
			}
		}
	}

	private fun setupStatusInfo() {
		certificatesViewModel.statefulWalletItems.observe(viewLifecycleOwner) { items ->
			items.filterIsInstance(StatefulWalletItem.VerifiedCertificate::class.java)
				.find { it.certificateHolder?.qrCodeData == certificateHolder.qrCodeData }?.let {
					updateStatusInfo(it.state)
				}
		}

		certificatesViewModel.startVerification(certificateHolder)
	}

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return
		when (state) {
			is VerificationState.LOADING -> displayLoadingState()
			is VerificationState.SUCCESS -> displaySuccessState()
			is VerificationState.INVALID -> displayInvalidState(state)
			is VerificationState.ERROR -> displayErrorState(state)
		}

		changeAlpha(state.getQrAlpha())
		setCertificateDetailTextColor(state.getNameDobColor())
	}

	private fun displayLoadingState() {
		showLoadingIndicator(true)
		setVerificationStateBubbleColor(R.color.greyish)
		binding.certificatePageStatusInfo.setText(R.string.wallet_certificate_verifying)
		binding.certificatePageStatusInfoRedBorder.visibility = View.GONE
	}

	private fun displaySuccessState() {
		showLoadingIndicator(false)
		setVerificationStateBubbleColor(R.color.blueish)
		binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_flag_ch)
		binding.certificatePageStatusInfoRedBorder.visibility = View.VISIBLE
		binding.certificatePageStatusInfo.setText(R.string.verifier_verify_success_certificate_light_info)
	}

	private fun displayInvalidState(state: VerificationState.INVALID) {
		showLoadingIndicator(false)
		setVerificationStateBubbleColor(R.color.greyish)
		binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_error_grey)
		binding.certificatePageStatusInfoRedBorder.visibility = View.GONE
		binding.certificatePageStatusInfo.text = state.getValidationStatusString(requireContext())
	}

	private fun displayErrorState(state: VerificationState.ERROR) {
		showLoadingIndicator(false)
		setVerificationStateBubbleColor(R.color.greyish)
		binding.certificatePageStatusInfoRedBorder.visibility = View.GONE
		if (state.isOfflineMode()) {
			binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_offline)
			binding.certificatePageStatusInfo.text = getString(R.string.wallet_homescreen_offline).makeBold()
		} else {
			binding.certificatePageStatusIcon.setImageResource(R.drawable.ic_process_error_grey)
			binding.certificatePageStatusInfo.text = getString(R.string.wallet_homescreen_network_error).makeBold()
		}
	}

	private fun showLoadingIndicator(isLoading: Boolean) {
		binding.certificatePageStatusLoading.isVisible = isLoading
		binding.certificatePageStatusIcon.isVisible = !isLoading
	}

	private fun setVerificationStateBubbleColor(@ColorRes colorId: Int) {
		val color = ContextCompat.getColor(requireContext(), colorId)
		binding.certificatePageStatusInfo.animateBackgroundTintColor(color)
	}

	private fun changeAlpha(alpha: Float) {
		binding.certificatePageQrCode.alpha = alpha
		binding.certificateLightPageValidity.alpha = alpha
	}

	private fun setCertificateDetailTextColor(@ColorRes colorId: Int) {
		val textColor = ContextCompat.getColor(requireContext(), colorId)
		binding.certificatePageName.setTextColor(textColor)
		binding.certificatePageBirthdate.setTextColor(textColor)
	}

	private fun deleteCertificateLightAndReloadWalletData() {
		certificateLightViewModel.deleteCertificateLight(certificateHolder)
		certificatesViewModel.loadWalletData()
	}

}