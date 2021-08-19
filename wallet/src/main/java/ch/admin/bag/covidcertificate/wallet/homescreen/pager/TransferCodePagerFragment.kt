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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.common.views.setCutOutCardBackground
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodePagerBinding
import ch.admin.bag.covidcertificate.wallet.transfercode.TransferCodeViewModel
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.view.TransferCodeBubbleView

class TransferCodePagerFragment : Fragment(R.layout.fragment_transfer_code_pager) {

	companion object {
		private const val ARG_TRANSFER_CODE = "ARG_TRANSFER_CODE"

		fun newInstance(transferCode: TransferCodeModel) = TransferCodePagerFragment().apply {
			arguments = bundleOf(ARG_TRANSFER_CODE to transferCode)
		}
	}

	private var _binding: FragmentTransferCodePagerBinding? = null
	private val binding get() = _binding!!

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val transferCodeViewModel by viewModels<TransferCodeViewModel>()
	private var transferCode: TransferCodeModel? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		transferCode = (arguments?.getSerializable(ARG_TRANSFER_CODE) as? TransferCodeModel)
			?: throw IllegalStateException("${this::class.java.simpleName} created without a transfer code argument!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentTransferCodePagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val transferCode = transferCode ?: return

		binding.transferCodePageCard.setCutOutCardBackground()
		binding.transferCodePageBubble.setTransferCode(transferCode)
		setTransferCodeViewState(false)

		binding.transferCodePageCard.setOnClickListener { certificatesViewModel.onTransferCodeClicked(transferCode) }

		transferCodeViewModel.conversionState.observe(viewLifecycleOwner) { onConversionStateChanged(it) }

		transferCodeViewModel.downloadCertificateForTransferCode(transferCode)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setTransferCodeViewState(isRefreshing: Boolean, error: StateError? = null) {
		val transferCode = transferCode ?: return
		TransitionManager.beginDelayedTransition(binding.root)
		when {
			transferCode.isFailed() -> {
				binding.transferCodePageWaitingImage.isVisible = false
				binding.transferCodePageImage.isVisible = true
				binding.transferCodePageImage.setImageResource(R.drawable.illu_transfer_code_failed)
				binding.transferCodePageStatusLabel.text = requireContext().getString(R.string.wallet_transfer_code_state_expired)
				binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(true, error))
			}
			transferCode.isExpired() -> {
				binding.transferCodePageWaitingImage.isVisible = true
				binding.transferCodePageImage.isVisible = false
				binding.transferCodePageStatusLabel.text = requireContext().getString(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(false, error))
			}
			else -> {
				binding.transferCodePageWaitingImage.isVisible = true
				binding.transferCodePageImage.isVisible = false
				binding.transferCodePageStatusLabel.text = requireContext().getString(R.string.wallet_transfer_code_state_waiting)
				if (error == null || error.code == ErrorCodes.GENERAL_OFFLINE) {
					binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Valid(isRefreshing,
						error))
				} else {
					binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Error(error))
				}
			}
		}
	}

	private fun onConversionStateChanged(state: TransferCodeConversionState) {
		when (state) {
			is TransferCodeConversionState.LOADING -> {
				setTransferCodeViewState(true)
			}
			is TransferCodeConversionState.CONVERTED -> {
				// Reload the wallet data to make sure the homescreen gets updated
				certificatesViewModel.loadWalletData()
			}
			is TransferCodeConversionState.NOT_CONVERTED -> {
				transferCode = transferCode?.let {
					certificatesViewModel.updateTransferCodeLastUpdated(it)
				}
				setTransferCodeViewState(false)
			}
			is TransferCodeConversionState.ERROR -> {
				setTransferCodeViewState(false, state.error)
			}
		}
	}
}