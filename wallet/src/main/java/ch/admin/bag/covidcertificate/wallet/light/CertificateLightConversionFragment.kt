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
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import ch.admin.bag.covidcertificate.common.util.makeSubStringBold
import ch.admin.bag.covidcertificate.common.util.makeSubStringsBold
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificateLightConversionBinding
import ch.admin.bag.covidcertificate.wallet.light.model.CertificateLightConversionState

class CertificateLightConversionFragment : Fragment(R.layout.fragment_certificate_light_conversion) {

	companion object {
		private const val ARG_DCC_HOLDER = "ARG_DCC_HOLDER"

		fun newInstance(certificateHolder: CertificateHolder) = CertificateLightConversionFragment().apply {
			arguments = bundleOf(ARG_DCC_HOLDER to certificateHolder)
		}
	}

	private val viewModel by viewModels<CertificateLightViewModel>()
	private var _binding: FragmentCertificateLightConversionBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificateHolder: CertificateHolder

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_DCC_HOLDER) as? CertificateHolder)
			?: throw IllegalArgumentException("Certificate light fragment created without a DccHolder!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificateLightConversionBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		setConversionInfo()

		viewModel.conversionState.observe(viewLifecycleOwner) { onConversionStateChanged(it) }

		binding.certificateLightConversionActivateButton.setOnClickListener { viewModel.convert(certificateHolder) }
		binding.certificateLightConversionRetryButton.setOnClickListener { viewModel.convert(certificateHolder) }
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setConversionInfo() {
		val text = getString(R.string.wallet_certificate_light_detail_text_2)
		val boldParts = getString(R.string.wallet_certificate_light_detail_text_2_bold)
		binding.certificateLightConversionInfo.text = text.makeSubStringsBold(boldParts.split(" "))
	}

	private fun onConversionStateChanged(state: CertificateLightConversionState) {
		when (state) {
			is CertificateLightConversionState.LOADING -> {
				binding.certificateLightConversionLoadingIndicator.isVisible = true
				binding.certificateLightConversionContent.isVisible = false
			}
			is CertificateLightConversionState.SUCCESS -> {
				// When the certificate is converted successfully, pop the backstack back to the home screen and open the
				// certificate light detail fragment without an animation
				parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
				parentFragmentManager.beginTransaction()
					.setCustomAnimations(0, R.anim.slide_exit, 0, R.anim.slide_pop_exit)
					.replace(
						R.id.fragment_container,
						CertificateLightDetailFragment.newInstance(state.certificateHolder, state.qrCodeImage)
					)
					.addToBackStack(CertificateLightDetailFragment::class.java.canonicalName)
					.commit()
			}
			is CertificateLightConversionState.RATE_LIMIT_EXCEEDED -> {
				binding.certificateLightConversionLoadingIndicator.isVisible = false
				binding.certificateLightConversionContent.isVisible = true
				binding.certificateLightConversionIntroLayout.isVisible = false
				binding.certificateLightConversionErrorLayout.isVisible = true
				binding.certificateLightConversionErrorCode.text = CertificateLightErrorCodes.RATE_LIMIT_EXCEEDED

				setStatusIconAndTint(R.drawable.ic_error)
				setStatusText(R.string.wallet_certificate_light_rate_limit_title, R.string.wallet_certificate_light_rate_limit_text)
			}
			is CertificateLightConversionState.ERROR -> {
				binding.certificateLightConversionLoadingIndicator.isVisible = false
				binding.certificateLightConversionContent.isVisible = true
				binding.certificateLightConversionIntroLayout.isVisible = false
				binding.certificateLightConversionErrorLayout.isVisible = true
				binding.certificateLightConversionErrorCode.text = state.error.code

				if (state.error.code == ErrorCodes.GENERAL_OFFLINE) {
					setStatusIconAndTint(R.drawable.ic_no_connection)
					setStatusText(
						R.string.wallet_certificate_light_detail_activation_network_error_title,
						R.string.wallet_certificate_light_detail_activation_network_error_text
					)
				} else {
					setStatusIconAndTint(R.drawable.ic_process_error)
					setStatusText(
						R.string.wallet_certificate_light_detail_activation_general_error_title,
						R.string.wallet_certificate_light_detail_activation_general_error_text
					)
				}
			}
		}
	}

	private fun setStatusIconAndTint(@DrawableRes iconId: Int, @ColorRes colorId: Int = R.color.orange) {
		val color = ContextCompat.getColor(requireContext(), colorId)
		binding.certificateLightConversionStatusIcon.setImageResource(iconId)
		binding.certificateLightConversionStatusIcon.imageTintList = ColorStateList.valueOf(color)
	}

	@SuppressLint("SetTextI18n")
	private fun setStatusText(@StringRes titleId: Int, @StringRes textId: Int) {
		val title = getString(titleId)
		val text = getString(textId)
		binding.certificateLightConversionStatusText.text = "$title\n$text".makeSubStringBold(title)
	}

}