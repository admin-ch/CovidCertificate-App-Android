/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.admin.bag.covidcertificate.common.qr.QrScanFragment
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.models.VerifierCertificateHolder
import ch.admin.bag.covidcertificate.sdk.android.verification.state.VerifierDecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentQrScanBinding
import ch.admin.bag.covidcertificate.verifier.verification.VerificationFragment


class VerifierQrScanFragment : QrScanFragment() {

	companion object {
		val TAG = VerifierQrScanFragment::class.java.canonicalName

		fun newInstance(): VerifierQrScanFragment {
			return VerifierQrScanFragment()
		}

	}

	private var _binding: FragmentQrScanBinding? = null
	private val binding get() = _binding!!

	override val viewFinderErrorColor: Int = R.color.red_error_qr_verifier
	override val viewFinderColor: Int = R.color.white
	override val torchOnDrawable: Int = R.drawable.ic_light_on_black
	override val torchOffDrawable: Int = R.drawable.ic_light_off
	override val zoomOnDrawable: Int = R.drawable.ic_zoom_on_black
	override val zoomOffDrawable: Int = R.drawable.ic_zoom_off_white

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)

		_binding = FragmentQrScanBinding.inflate(inflater, container, false)

		toolbar = binding.fragmentQrScannerToolbar
		qrCodeScanner = binding.qrCodeScanner
		cutOut = binding.cameraPreviewContainer
		flashButton = binding.fragmentQrScannerFlashButton
		errorView = binding.fragmentQrScannerErrorView
		errorCodeView = binding.qrCodeScannerErrorCode
		zoomButton = binding.fragmentQrZoom

		invalidCodeText = binding.qrCodeScannerInvalidCodeText
		viewFinderTopLeftIndicator = binding.qrCodeScannerTopLeftIndicator
		viewFinderTopRightIndicator = binding.qrCodeScannerTopRightIndicator
		viewFinderBottomLeftIndicator = binding.qrCodeScannerBottomLeftIndicator
		viewFinderBottomRightIndicator = binding.qrCodeScannerBottomRightIndicator

		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	override fun decodeQrCodeData(qrCodeData: String, onDecodeSuccess: () -> Unit, onDecodeError: (StateError) -> Unit) {
		when (val decodeState = CovidCertificateSdk.Verifier.decode(qrCodeData)) {
			is VerifierDecodeState.SUCCESS -> {
				onDecodeSuccess.invoke()
				showVerificationFragment(decodeState.certificateHolder)
			}
			is VerifierDecodeState.ERROR -> onDecodeError.invoke(decodeState.error)
		}
	}

	private fun showVerificationFragment(certificateHolder: VerifierCertificateHolder) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, VerificationFragment.newInstance(certificateHolder))
			.addToBackStack(VerificationFragment::class.java.canonicalName)
			.commit()
	}

}

