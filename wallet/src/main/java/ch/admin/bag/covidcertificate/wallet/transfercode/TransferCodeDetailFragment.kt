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
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.config.FaqModel
import ch.admin.bag.covidcertificate.common.faq.FaqAdapter
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.IntroSection
import ch.admin.bag.covidcertificate.common.faq.model.Question
import ch.admin.bag.covidcertificate.common.util.setSecureFlagToBlockScreenshots
import ch.admin.bag.covidcertificate.common.views.rotate
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodeDetailBinding
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.view.TransferCodeBubbleView

class TransferCodeDetailFragment : Fragment(R.layout.fragment_transfer_code_detail) {

	companion object {
		private const val ARG_TRANSFER_CODE = "ARG_TRANSFER_CODE"

		fun newInstance(transferCode: TransferCodeModel) = TransferCodeDetailFragment().apply {
			arguments = bundleOf(ARG_TRANSFER_CODE to transferCode)
		}
	}

	private var _binding: FragmentTransferCodeDetailBinding? = null
	private val binding get() = _binding!!

	private val configViewModel by activityViewModels<ConfigViewModel>()
	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val transferCodeViewModel by activityViewModels<TransferCodeViewModel>()
	private val faqAdapter = FaqAdapter()
	private lateinit var transferCode: TransferCodeModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		transferCode = (arguments?.getSerializable(ARG_TRANSFER_CODE) as? TransferCodeModel)
			?: throw IllegalStateException("${this::class.java.simpleName} created without a transfer code argument!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentTransferCodeDetailBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		binding.transferCodeDetailFaqList.adapter = faqAdapter

		binding.transferCodeDetailBubble.setTransferCode(transferCode)
		setTransferCodeBubbleViewState()

		binding.transferCodeRefreshButton.setOnClickListener { onRefreshButtonClicked() }
		binding.transferCodeDetailDeleteButton.setOnClickListener { onDeleteButtonClicked() }

		configViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			val languageKey = getString(R.string.language_key)
			config.getTransferWorksFaqs(languageKey)?.let {
				showTransferCodeFaqItems(it)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setTransferCodeBubbleViewState() {
		when {
			transferCode.isFailed() -> {
				binding.transferCodeDetailIllu.setImageResource(R.drawable.illu_transfer_code_failed)
				binding.transferCodeDetailTitle.text = requireContext().getString(R.string.wallet_transfer_code_state_expired)
				binding.transferCodeDetailBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(true))
			}
			transferCode.isExpired() -> {
				binding.transferCodeDetailTitle.text = requireContext().getString(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodeDetailBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(false))
			}
			else -> {
				binding.transferCodeDetailTitle.text = requireContext().getString(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodeDetailBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Valid(false))
			}
		}
	}

	private fun onRefreshButtonClicked() {
		binding.transferCodeRefreshButton.rotate(360f, 1000L, 0f)
		// TODO Call backend to check if certificate has arrived yet
	}

	private fun onDeleteButtonClicked() {
		AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
			.setTitle(R.string.delete_button)
			.setMessage(R.string.wallet_transfer_delete_confirm_text)
			.setPositiveButton(R.string.delete_button) { _, _ ->
				certificatesViewModel.removeTransferCode(transferCode)
				parentFragmentManager.popBackStack()
			}
			.setNegativeButton(R.string.cancel_button) { dialog, _ ->
				dialog.dismiss()
			}
			.setCancelable(true)
			.create()
			.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
			.show()
	}

	private fun showTransferCodeFaqItems(faqModel: FaqModel) {
		val header = listOf(Header(faqModel.faqIconAndroid, faqModel.faqTitle, faqModel.faqSubTitle))
		val introSections = faqModel.faqIntroSections?.map { IntroSection(it.iconAndroid, it.text) } ?: emptyList()
		val questions = faqModel.faqEntries?.map { Question(it.title, it.text, false, it.linkTitle, it.linkUrl) } ?: emptyList()
		val adapterItems: List<Faq> = listOf(header, introSections, questions).flatten()
		faqAdapter.setItems(adapterItems)
	}

}