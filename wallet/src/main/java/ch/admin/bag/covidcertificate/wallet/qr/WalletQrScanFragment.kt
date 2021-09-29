/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.admin.bag.covidcertificate.common.aws.AWSRepository
import ch.admin.bag.covidcertificate.common.aws.AWSSpec
import ch.admin.bag.covidcertificate.common.qr.QrScanFragment
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.add.CertificateAddFragment
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentQrScanBinding
import ch.admin.bag.covidcertificate.wallet.howto.HowToScanFragment


class WalletQrScanFragment : QrScanFragment() {

	companion object {
		val TAG = WalletQrScanFragment::class.java.canonicalName

		fun newInstance(): WalletQrScanFragment {
			return WalletQrScanFragment()
		}
	}

	private var _binding: FragmentQrScanBinding? = null
	private val binding get() = _binding!!

	override val viewFinderErrorColor: Int = R.color.red_error_qr_wallet
	override val viewFinderColor: Int = R.color.blue
	override val torchOnDrawable: Int = R.drawable.ic_light_on
	override val torchOffDrawable: Int = R.drawable.ic_light_off_blue

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)

		_binding = FragmentQrScanBinding.inflate(inflater, container, false)

		repository = AWSRepository.getInstance(AWSSpec(requireContext().applicationContext, BuildConfig.AWS_API_KEY))
		toolbar = binding.fragmentQrScannerToolbar
		qrCodeScanner = binding.qrCodeScanner
		flashButton = binding.fragmentQrScannerFlashButton
		errorView = binding.fragmentQrScannerErrorView
		errorCodeView = binding.qrCodeScannerErrorCode
		uploadButton = binding.fragmentUploadButton

		invalidCodeText = binding.qrCodeScannerInvalidCodeText
		cutOut = binding.cameraPreviewContainer
		viewFinderTopLeftIndicator = binding.qrCodeScannerTopLeftIndicator
		viewFinderTopRightIndicator = binding.qrCodeScannerTopRightIndicator
		viewFinderBottomLeftIndicator = binding.qrCodeScannerBottomLeftIndicator
		viewFinderBottomRightIndicator = binding.qrCodeScannerBottomRightIndicator

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.qrCodeScannerButtonHow.setOnClickListener { showHowToScanFragment() }
	}

	override fun decodeQrCodeData(qrCodeData: String, onDecodeSuccess: () -> Unit, onDecodeError: (StateError) -> Unit) {
		when (val decodeState = CovidCertificateSdk.Wallet.decode(qrCodeData)) {
			is DecodeState.SUCCESS -> {
				val certificateHolder = decodeState.certificateHolder

				// If a certificate light was decoded, treat it as a prefix decode error to prevent a certificate light being added
				if (certificateHolder.containsChLightCert()) {
					onDecodeError.invoke(StateError(ErrorCodes.DECODE_PREFIX))
				} else {
					onDecodeSuccess.invoke()
					showCertificationAddFragment(decodeState.certificateHolder)
				}
			}
			is DecodeState.ERROR -> onDecodeError.invoke(decodeState.error)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun showCertificationAddFragment(certificateHolder: CertificateHolder) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, CertificateAddFragment.newInstance(certificateHolder, true))
			.addToBackStack(CertificateAddFragment::class.java.canonicalName)
			.commit()
	}

	private fun showHowToScanFragment() {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, HowToScanFragment.newInstance())
			.addToBackStack(HowToScanFragment::class.java.canonicalName)
			.commit()
	}

}