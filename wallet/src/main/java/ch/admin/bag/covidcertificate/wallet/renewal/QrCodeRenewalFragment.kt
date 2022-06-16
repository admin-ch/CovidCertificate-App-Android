/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.renewal

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import ch.admin.bag.covidcertificate.common.config.CertificateRenewalType
import ch.admin.bag.covidcertificate.common.extensions.collectWhenStarted
import ch.admin.bag.covidcertificate.common.extensions.getDrawableIdentifier
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.views.animateBackgroundTintColor
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentQrCodeRenewalBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemIconTextInfoBinding
import ch.admin.bag.covidcertificate.wallet.renewal.model.QrCodeRenewalViewState
import java.time.ZoneOffset

class QrCodeRenewalFragment : Fragment() {

	companion object {
		const val REQUEST_KEY_CERTIFICATE = "REQUEST_KEY_CERTIFICATE"
		const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificateHolder: CertificateHolder) = QrCodeRenewalFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificateHolder)
		}
	}

	private var _binding: FragmentQrCodeRenewalBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificateHolder: CertificateHolder

	private val certificatesViewModel by activityViewModels<CertificatesAndConfigViewModel>()
	private val viewModel by viewModels<QrCodeRenewalViewModel>()

	private var hasUserRenewedCertificate = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? CertificateHolder)
			?: throw IllegalStateException("QR Code Renewal fragment created without Certificate!")

		viewModel.setCertificate(certificateHolder)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentQrCodeRenewalBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		showCertificateExpirationDate()

		binding.qrCodeRenewalButton.setOnClickListener {
			hasUserRenewedCertificate = true
			viewModel.renewCertificate(certificateHolder)
		}

		binding.qrCodeRenewalFaqButton.setOnClickListener {
			val url = getString(R.string.wallet_certificate_renewal_faq_link_url) // TODO Get from config
			UrlUtil.openUrl(requireContext(), url)
		}

		viewLifecycleOwner.collectWhenStarted(viewModel.viewState) {
			when (it) {
				is QrCodeRenewalViewState.RenewalRequired -> showRenewalRequired()
				is QrCodeRenewalViewState.RenewalInProgress -> showRenewalInProgress()
				is QrCodeRenewalViewState.RenewalSuccessful -> showRenewalSuccessful(it)
				is QrCodeRenewalViewState.RenewalFailed -> showRenewalFailed(it)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun showCertificateExpirationDate() {
		val expirationDateTime = certificateHolder.expirationTime
			?.atOffset(ZoneOffset.UTC)
			?.toLocalDateTime()
			?.format(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)
		binding.qrCodeExpirationDate.text = expirationDateTime
	}

	private fun showInfoList(renewalType: CertificateRenewalType) {
		val infos = ConfigRepository.getCurrentConfig(requireContext())
			?.getCertRenewalInfo(getString(R.string.language_key))
			?.get(renewalType)


		if (infos != null) {
			binding.qrCodeRenewalInfoTitle.isVisible = true
			binding.qrCodeRenewalInfoTitle.text = infos.heading
			binding.qrCodeRenewalInfos.isVisible = true
			binding.qrCodeRenewalInfos.removeAllViews()
			// TODO Set faq button text

			val inflater = LayoutInflater.from(requireContext())
			infos.infos.forEach { info ->
				val item = ItemIconTextInfoBinding.inflate(inflater, binding.qrCodeRenewalInfos, true)
				val iconId = requireContext().getDrawableIdentifier(info.iconAndroid)
				item.hintIcon.setImageResource(iconId)
				item.hintText.text = info.text
			}
		} else {
			binding.qrCodeRenewalInfos.isVisible = false
			binding.qrCodeRenewalInfoTitle.isVisible = false
			binding.qrCodeRenewalFaqButton.isVisible = false
		}
	}

	private fun showRenewalRequired() {
		val backgroundColor = ContextCompat.getColor(requireContext(), R.color.redish)
		binding.qrCodeExpirationBubble.animateBackgroundTintColor(backgroundColor)
		binding.qrCodeRenewalStateInfo.animateBackgroundTintColor(backgroundColor)
		binding.qrCodeRenewalStateIcon.setImageResource(R.drawable.ic_error)
		binding.qrCodeRenewalStateIcon.isVisible = true
		binding.qrCodeRenewalStateInfo.setText(R.string.wallet_certificate_renewal_required_info)
		binding.qrCodeRenewalLoadingIndicator.isVisible = false
		binding.qrCodeRenewalButton.isVisible = true
		binding.qrCodeRenewalButton.isEnabled = true
		binding.qrCodeRenewalButton.setText(R.string.wallet_certificate_renew_now_button)
		binding.certificateDetailErrorCode.isVisible = false
		showInfoList(CertificateRenewalType.EXPIRED)
	}

	private fun showRenewalInProgress() {
		val backgroundColor = ContextCompat.getColor(requireContext(), R.color.greyish)
		binding.qrCodeExpirationBubble.animateBackgroundTintColor(backgroundColor)
		binding.qrCodeRenewalStateInfo.animateBackgroundTintColor(backgroundColor)
		binding.qrCodeRenewalStateIcon.isVisible = false
		binding.qrCodeRenewalStateInfo.setText(R.string.wallet_certificate_renewal_in_progress_info)
		binding.qrCodeRenewalLoadingIndicator.isVisible = true
		binding.qrCodeRenewalButton.isVisible = true
		binding.qrCodeRenewalButton.isEnabled = false
		binding.certificateDetailErrorCode.isVisible = false
		showInfoList(CertificateRenewalType.EXPIRED)
	}

	private fun showRenewalSuccessful(state: QrCodeRenewalViewState.RenewalSuccessful) {
		val backgroundColor = ContextCompat.getColor(requireContext(), R.color.greenish)
		val iconColor = ContextCompat.getColor(requireContext(), R.color.green)
		binding.qrCodeExpirationBubble.animateBackgroundTintColor(backgroundColor)
		binding.qrCodeRenewalStateInfo.animateBackgroundTintColor(backgroundColor)
		binding.qrCodeRenewalStateIcon.setImageResource(R.drawable.ic_check_filled)
		binding.qrCodeRenewalStateIcon.imageTintList = ColorStateList.valueOf(iconColor)
		binding.qrCodeRenewalStateIcon.isVisible = true
		binding.qrCodeRenewalStateInfo.setText(R.string.wallet_certificate_renewal_successful_info)
		binding.qrCodeRenewalLoadingIndicator.isVisible = false
		binding.qrCodeRenewalButton.isVisible = false
		binding.certificateDetailErrorCode.isVisible = false
		showInfoList(CertificateRenewalType.RENEWED)

		if (hasUserRenewedCertificate) {
			// If the user has actually renewed the certificate, reload the wallet data (because the certificate has become
			// effectively a new certificate due to the new QR code data), decode the new qr code data and set it as the fragment result
			certificatesViewModel.loadWalletData()
			hasUserRenewedCertificate = false

			val decodeState = CertificateDecoder.decode(state.newQrCodeData)
			if (decodeState is DecodeState.SUCCESS) {
				setFragmentResult(REQUEST_KEY_CERTIFICATE, bundleOf(ARG_CERTIFICATE to decodeState.certificateHolder))
			}
		}
	}

	private fun showRenewalFailed(state: QrCodeRenewalViewState.RenewalFailed) {
		val expirationColor = ContextCompat.getColor(requireContext(), R.color.greyish)
		val stateColor = ContextCompat.getColor(requireContext(), R.color.orangeish)
		binding.qrCodeExpirationBubble.animateBackgroundTintColor(expirationColor)
		binding.qrCodeRenewalStateInfo.animateBackgroundTintColor(stateColor)
		binding.qrCodeRenewalLoadingIndicator.isVisible = false
		binding.qrCodeRenewalButton.isVisible = true
		binding.qrCodeRenewalButton.isEnabled = true
		binding.qrCodeRenewalButton.setText(R.string.error_action_retry)
		binding.certificateDetailErrorCode.isVisible = true
		binding.certificateDetailErrorCode.text = state.error.code

		when (state.error.code) {
			ErrorCodes.GENERAL_OFFLINE -> {
				binding.qrCodeRenewalStateIcon.setImageResource(R.drawable.ic_no_connection)
				binding.qrCodeRenewalStateInfo.text = buildSpannedString {
					bold {
						append(getString(R.string.wallet_certificate_renewal_offline_error_title))
					}
					appendLine()
					append(getString(R.string.wallet_certificate_renewal_offline_error_text))
				}
			}
			QrCodeRenewalErrorCodes.RATE_LIMIT_EXCEEDED -> {
				binding.qrCodeRenewalStateIcon.setImageResource(R.drawable.ic_error_orange)
				binding.qrCodeRenewalStateInfo.text = buildSpannedString {
					bold {
						append(getString(R.string.wallet_certificate_renewal_rate_limit_error_title))
					}
					appendLine()
					append(getString(R.string.wallet_certificate_renewal_rate_limit_error_text))
				}
			}
			else -> {
				binding.qrCodeRenewalStateIcon.setImageResource(R.drawable.ic_process_error)
				binding.qrCodeRenewalStateInfo.text = buildSpannedString {
					bold {
						append(getString(R.string.wallet_certificate_renewal_general_error_title))
					}
					appendLine()
					append(getString(R.string.wallet_certificate_renewal_general_error_text))
				}
			}
		}
		showInfoList(CertificateRenewalType.EXPIRED)

		hasUserRenewedCertificate = false
	}

}