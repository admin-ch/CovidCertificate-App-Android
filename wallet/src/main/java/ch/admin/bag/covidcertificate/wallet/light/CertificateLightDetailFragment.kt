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
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.prettyPrintIsoDateTime
import ch.admin.bag.covidcertificate.sdk.core.extensions.fromBase64
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificateLightDetailBinding
import ch.admin.bag.covidcertificate.wallet.util.getValidationStatusString
import ch.admin.bag.covidcertificate.wallet.util.isOfflineMode
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

class CertificateLightDetailFragment : Fragment(R.layout.fragment_certificate_light_detail) {

	companion object {
		private const val ARG_CERTIFICATE_HOLDER = "ARG_CERTIFICATE_HOLDER"
		private const val ARG_QR_CODE_IMAGE = "ARG_QR_CODE_IMAGE"

		fun newInstance(certificateHolder: CertificateHolder, qrCodeImage: String) = CertificateLightDetailFragment().apply {
			arguments = bundleOf(
				ARG_CERTIFICATE_HOLDER to certificateHolder,
				ARG_QR_CODE_IMAGE to qrCodeImage
			)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val certificateLightViewModel by viewModels<CertificateLightViewModel>()
	private var _binding: FragmentCertificateLightDetailBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificateHolder: CertificateHolder
	private lateinit var qrCodeImage: String

	private var validityTimer: Timer? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE_HOLDER) as? CertificateHolder)
			?: throw IllegalArgumentException("Certificate light detail fragment created without a DccHolder!")
		qrCodeImage = arguments?.getString(ARG_QR_CODE_IMAGE, null)
			?: throw IllegalArgumentException("Certificate light detail fragment created without a qr code image!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificateLightDetailBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		displayQrCode()
		displayValidity()
		displayCertificateDetails()
		setupStatusInfo()

		binding.certificateLightDetailDeactivateButton.setOnClickListener {
			certificateLightViewModel.deleteCertificateLight(certificateHolder)
			parentFragmentManager.popBackStack()
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
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificateLightDetailQrCode.setImageDrawable(qrCodeDrawable)
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
				binding.certificateLightDetailValidity.text = "%02d:%02d:%02d".format(hours, minutes, seconds)
			}

			if (remainingTimeInSeconds <= 0) {
				validityTimer?.cancel()
				validityTimer = null
			}
		}
	}

	private fun displayCertificateDetails() {
		val chLightCert = certificateHolder.certificate as? ChLightCert ?: return

		val name = "${chLightCert.person.familyName} ${chLightCert.person.givenName}"
		binding.certificateLightDetailName.text = name
		val dateOfBirth = chLightCert.dateOfBirth.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificateLightDetailBirthdate.text = dateOfBirth
	}

	private fun setupStatusInfo() {
		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { certificates ->
			certificates.find { it.certificateHolder?.qrCodeData == certificateHolder.qrCodeData }?.let {
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
	}

	private fun displayLoadingState() {
		showLoadingIndicator(true)
		setVerificationStateBubbleColor(R.color.greyish)
		binding.certificateLightDetailVerificationStatus.setText(R.string.wallet_certificate_verifying)

	}

	private fun displaySuccessState() {
		showLoadingIndicator(false)
		setVerificationStateBubbleColor(R.color.blueish)
		binding.certificateLightDetailVerificationStatus.setText(R.string.verifier_verify_success_certificate_light_info)
	}

	private fun displayInvalidState(state: VerificationState.INVALID) {
		showLoadingIndicator(false)
		setVerificationStateBubbleColor(R.color.greyish)
		binding.certificateLightDetailVerificationStatus.text = state.getValidationStatusString(requireContext())

	}

	private fun displayErrorState(state: VerificationState.ERROR) {
		showLoadingIndicator(false)
		setVerificationStateBubbleColor(R.color.greyish)
		if (state.isOfflineMode()) {
			binding.certificateLightDetailVerificationStatus.text = getString(R.string.wallet_homescreen_offline).makeBold()
		} else {
			binding.certificateLightDetailVerificationStatus.text = getString(R.string.wallet_homescreen_network_error).makeBold()
		}
	}

	private fun showLoadingIndicator(isLoading: Boolean) {
		binding.certificateLightDetailStatusLoading.isVisible = isLoading
		binding.certificateLightDetailStatusIcon.isVisible = !isLoading
	}

	private fun setVerificationStateBubbleColor(@ColorRes colorId: Int) {
		val color = ContextCompat.getColor(requireContext(), colorId)
		binding.certificateLightDetailVerificationStatus.animateBackgroundTintColor(color)
	}

}