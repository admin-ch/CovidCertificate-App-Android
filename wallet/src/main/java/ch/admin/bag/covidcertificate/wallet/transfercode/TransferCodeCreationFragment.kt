/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.eval.data.EvalErrorCodes
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodeCreationBinding
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeCreationState
import ch.admin.bag.covidcertificate.wallet.transfercode.view.TransferCodeBubbleView

class TransferCodeCreationFragment : Fragment(R.layout.fragment_transfer_code_creation) {

	companion object {
		fun newInstance() = TransferCodeCreationFragment()
	}

	private var _binding: FragmentTransferCodeCreationBinding? = null
	private val binding get() = _binding!!

	private val viewModel by viewModels<TransferCodeCreationViewModel>()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentTransferCodeCreationBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		binding.transferCodeCreationDoneButton.setOnClickListener { parentFragmentManager.popBackStack() }

		viewModel.creationState.observe(viewLifecycleOwner) { onViewStateChanged(it) }
		viewModel.createTransferCode()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun onViewStateChanged(state: TransferCodeCreationState) {
		TransitionManager.beginDelayedTransition(binding.root)
		when (state) {
			TransferCodeCreationState.LOADING -> {
				binding.transferCodeLoadingIndicator.isVisible = true
				binding.transferCodeContent.isVisible = false
			}
			is TransferCodeCreationState.SUCCESS -> {
				binding.transferCodeLoadingIndicator.isVisible = false
				binding.transferCodeContent.isVisible = true
				binding.transferCodeNextSteps.isVisible = true
				binding.transferCodeErrorLayout.isVisible = false
				binding.transferCodeCreationDoneLayout.isVisible = true

				binding.transferCodeCreationTitle.setText(R.string.wallet_transfer_code_code_created_title)
				binding.transferCodeBubble.setTransferCode(state.transferCode)
				binding.transferCodeBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Created)
			}
			is TransferCodeCreationState.ERROR -> {
				binding.transferCodeLoadingIndicator.isVisible = false
				binding.transferCodeContent.isVisible = true
				binding.transferCodeNextSteps.isVisible = false
				binding.transferCodeErrorLayout.isVisible = true
				binding.transferCodeCreationDoneLayout.isVisible = false

				binding.transferCodeCreationTitle.setText(R.string.wallet_transfer_code_error_title)
				val isOfflineError = state.error.code == EvalErrorCodes.GENERAL_OFFLINE
				binding.transferCodeBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Error(isOfflineError))
				binding.transferCodeErrorCode.text = state.error.code
				binding.transferCodeCreationRetryButton.setOnClickListener {
					viewModel.createTransferCode()
				}
			}
		}
	}
}