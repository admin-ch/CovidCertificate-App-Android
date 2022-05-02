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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import ch.admin.bag.covidcertificate.common.qr.QrScanFragment
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.models.VerifierCertificateHolder
import ch.admin.bag.covidcertificate.sdk.android.verification.state.VerifierDecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentQrScanBinding
import ch.admin.bag.covidcertificate.verifier.modes.ChooseModeDialogFragment
import ch.admin.bag.covidcertificate.verifier.modes.ModesAndConfigViewModel
import ch.admin.bag.covidcertificate.verifier.verification.VerificationFragment
import ch.admin.bag.covidcertificate.verifier.verification.VerificationFragment.Companion.RESULT_FRAGMENT_POPPED
import ch.admin.bag.covidcertificate.verifier.zebra.ZebraActionBroadcastReceiver

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

	private val verifierSecureStorage by lazy { VerifierSecureStorage.getInstance(requireContext()) }
	private val zebraBroadcastReceiver by lazy { ZebraActionBroadcastReceiver(verifierSecureStorage) }

	private val modesViewModel by activityViewModels<ModesAndConfigViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Restart the Analyzer whenever the VerificationFragment is popped
		setFragmentResultListener(RESULT_FRAGMENT_POPPED) { _, _ ->
			setScannerCallback()
		}
	}

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

		if (verifierSecureStorage.hasZebraScanner()) {
			// Needs to be before the onAttachedToWindow of the scanner view (which happens between onCreateView and onViewCreated)
			qrCodeScanner.setAutoActivateOnAttach(false)
		}

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (verifierSecureStorage.hasZebraScanner()) {
			binding.qrCodeScannerExternalHardwareDetected.isVisible = true
		} else {
			activateCamera()
		}

		modesViewModel.modesLiveData.observe(viewLifecycleOwner) { modeState ->
			val mode = modeState.selectedMode
			if (mode == null || modeState.availableModes.size <= 1) {
				binding.fragmentQrScannerModeIndicator.visibility = View.GONE
			} else {
				binding.fragmentQrScannerModeIndicator.backgroundTintList = ColorStateList.valueOf(mode.hexColor.toColorInt())
				binding.fragmentQrScannerModeIndicator.text = mode.title
				binding.fragmentQrScannerModeIndicator.visibility = View.VISIBLE
			}
			binding.fragmentQrScannerModeIndicator.setOnClickListener {
				ChooseModeDialogFragment.newInstance()
					.show(childFragmentManager, ChooseModeDialogFragment::class.java.canonicalName)
			}
		}

		setupActivateCameraButton()
	}

	override fun onResume() {
		super.onResume()

		val modeState = modesViewModel.modesLiveData.value
		if (modeState?.availableModes?.size ?: 0 > 1 && modeState?.selectedMode == null) {
			ChooseModeDialogFragment.newInstance().apply { isCancelable = false }
				.show(childFragmentManager, ChooseModeDialogFragment::class.java.canonicalName)
		}

		zebraBroadcastReceiver.registerWith(requireContext()) { decodeQrCodeData(it, {}, {}) }
	}

	override fun onPause() {
		super.onPause()
		zebraBroadcastReceiver.unregisterWith(requireContext())
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

	private fun setupActivateCameraButton() {
		binding.fragmentCameraActivate.isVisible = verifierSecureStorage.hasZebraScanner()

		binding.fragmentCameraActivate.setOnClickListener {
			if (qrCodeScanner.isCameraActive) {
				deactivateCamera()
			} else {
				activateCamera()
			}
			binding.fragmentCameraActivate.isSelected = !qrCodeScanner.isCameraActive

			val iconColor = if (qrCodeScanner.isCameraActive) R.color.white else R.color.black
			binding.fragmentCameraActivate.imageTintList =
				ColorStateList.valueOf(ContextCompat.getColor(requireContext(), iconColor))
		}
	}

	private fun showVerificationFragment(certificateHolder: VerifierCertificateHolder) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.add(R.id.fragment_container, VerificationFragment.newInstance(certificateHolder))
			.addToBackStack(VerificationFragment::class.java.canonicalName)
			.commit()
	}

}

