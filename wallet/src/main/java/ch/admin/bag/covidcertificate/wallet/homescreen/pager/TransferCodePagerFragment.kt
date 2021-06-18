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
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodePagerBinding
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.view.TransferCodeBubbleView
import ch.admin.bag.covidcertificate.common.util.CutOutEdgeTreatment
import ch.admin.bag.covidcertificate.common.views.setCutOutCardBackground
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

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
	private lateinit var transferCode: TransferCodeModel

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

		binding.transferCodePageCard.setCutOutCardBackground()
		binding.transferCodePageBubble.setTransferCode(transferCode)
		setTransferCodeBubbleViewState()

		binding.transferCodePageCard.setOnClickListener { certificatesViewModel.onTransferCodeClicked(transferCode) }
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setTransferCodeBubbleViewState() {
		when {
			transferCode.isFailed() -> {
				binding.transferCodePageImage.setImageResource(R.drawable.illu_transfer_code_failed)
				binding.transferCodePageStatusLabel.text = requireContext().getString(R.string.wallet_transfer_code_state_expired)
				binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(true))
			}
			transferCode.isExpired() -> {
				binding.transferCodePageStatusLabel.text = requireContext().getString(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(false))
			}
			else -> {
				binding.transferCodePageStatusLabel.text = requireContext().getString(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Valid(false))
			}
		}
	}
}