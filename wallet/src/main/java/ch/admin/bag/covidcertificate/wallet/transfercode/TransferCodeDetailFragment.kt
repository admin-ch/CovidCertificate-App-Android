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

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.config.FaqModel
import ch.admin.bag.covidcertificate.common.faq.FaqAdapter
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.IntroSection
import ch.admin.bag.covidcertificate.common.faq.model.Question
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.util.makeSubStringBold
import ch.admin.bag.covidcertificate.common.util.setSecureFlagToBlockScreenshots
import ch.admin.bag.covidcertificate.common.views.rotate
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes
import ch.admin.bag.covidcertificate.eval.data.state.Error
import ch.admin.bag.covidcertificate.eval.utils.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.eval.utils.prettyPrint
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodeDetailBinding
import ch.admin.bag.covidcertificate.wallet.detail.CertificateDetailFragment
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.view.TransferCodeBubbleView

class TransferCodeDetailFragment : Fragment(R.layout.fragment_transfer_code_detail) {

	companion object {
		private const val ARG_TRANSFER_CODE = "ARG_TRANSFER_CODE"
		private const val DATE_REPLACEMENT_STRING = "{DATE}"
		private const val FORCE_RELOAD_DELAY = 1000L

		fun newInstance(transferCode: TransferCodeModel) = TransferCodeDetailFragment().apply {
			arguments = bundleOf(ARG_TRANSFER_CODE to transferCode)
		}
	}

	private var _binding: FragmentTransferCodeDetailBinding? = null
	private val binding get() = _binding!!

	private val configViewModel by activityViewModels<ConfigViewModel>()
	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val transferCodeViewModel by viewModels<TransferCodeViewModel>()
	private val faqAdapter = FaqAdapter { url: String  -> context?.let { UrlUtil.openUrl(it, url) }}
	private var transferCode: TransferCodeModel? = null

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
		val transferCode = transferCode ?: return

		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		binding.transferCodeDetailFaqList.adapter = faqAdapter

		binding.transferCodeDetailBubble.setTransferCode(transferCode)
		setTransferCodeViewState(false)

		binding.transferCodeRefreshButton.setOnClickListener { onRefreshButtonClicked() }
		binding.transferCodeDetailDeleteButton.setOnClickListener { onDeleteButtonClicked() }

		configViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			val languageKey = getString(R.string.language_key)
			config.getTransferWorksFaqs(languageKey)?.let {
				showTransferCodeFaqItems(it)
			}
		}

		transferCodeViewModel.conversionState.observe(viewLifecycleOwner) { onConversionStateChanged(it) }

		transferCodeViewModel.downloadCertificateForTransferCode(transferCode)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setTransferCodeViewState(isRefreshing: Boolean, error: Error? = null) {
		val transferCode = transferCode ?: return
		TransitionManager.beginDelayedTransition(binding.root)
		when {
			transferCode.isFailed() -> {
				binding.transferCodeDetailWaitingImage.isVisible = false
				binding.transferCodeDetailImage.isVisible = true
				binding.transferCodeDetailImage.setImageResource(R.drawable.illu_transfer_code_failed)
				binding.transferCodeDetailTitle.setText(R.string.wallet_transfer_code_state_expired)
				binding.transferCodeDetailBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(true, error))
				binding.transferCodeDetailRefreshLayout.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.redish))
				binding.transferCodeRefreshButton.isVisible = false
				binding.transferCodeErrorCode.isVisible = false
				binding.transferCodeLastUpdate.setText(R.string.wallet_transfer_code_state_no_certificate)
			}
			transferCode.isExpired() -> {
				binding.transferCodeDetailWaitingImage.isVisible = true
				binding.transferCodeDetailImage.isVisible = false
				binding.transferCodeDetailTitle.setText(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodeDetailBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Expired(false, error))
				binding.transferCodeRefreshButton.isVisible = true
				binding.transferCodeErrorCode.isVisible = false
				showErrorOrLastUpdated(error)
			}
			else -> {
				binding.transferCodeDetailWaitingImage.isVisible = true
				binding.transferCodeDetailImage.isVisible = false
				binding.transferCodeDetailTitle.setText(R.string.wallet_transfer_code_state_waiting)
				binding.transferCodeDetailBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Valid(isRefreshing, error))
				binding.transferCodeRefreshButton.isVisible = true
				binding.transferCodeErrorCode.isVisible = error != null
				binding.transferCodeErrorCode.text = error?.code
				showErrorOrLastUpdated(error)
			}
		}
	}

	@SuppressLint("SetTextI18n")
	private fun showErrorOrLastUpdated(error: Error?) {
		val transferCode = transferCode ?: return
		if (error != null) {
			binding.transferCodeDetailRefreshLayout.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.orangeish))
			binding.transferCodeRefreshButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.orange))
			binding.transferCodeRefreshButton.setImageResource(R.drawable.ic_retry)

			if (error.code == ErrorCodes.GENERAL_OFFLINE) {
				val offlineTitle = getString(R.string.wallet_transfer_code_no_internet_title)
				val offlineText = getString(R.string.wallet_transfer_code_update_no_internet_error_text)
				binding.transferCodeLastUpdate.text = "$offlineTitle\n$offlineText".makeSubStringBold(offlineTitle)
			} else {
				val errorTitle = getString(R.string.wallet_transfer_code_update_error_title)
				val errorText = getString(R.string.wallet_transfer_code_update_general_error_text)
				binding.transferCodeLastUpdate.text = "$errorTitle\n$errorText".makeSubStringBold(errorTitle)
			}
		} else {
			binding.transferCodeDetailRefreshLayout.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blueish))
			binding.transferCodeRefreshButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue))
			binding.transferCodeRefreshButton.setImageResource(R.drawable.ic_load)
			val lastUpdated = transferCode.lastUpdatedTimestamp.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)
			binding.transferCodeLastUpdate.text = getString(R.string.wallet_transfer_code_state_updated)
				.replace(DATE_REPLACEMENT_STRING, lastUpdated).makeSubStringBold(lastUpdated)
		}
	}

	private fun onConversionStateChanged(state: TransferCodeConversionState) {
		when (state) {
			is TransferCodeConversionState.LOADING -> {
				binding.transferCodeLoadingIndicator.isVisible = true
				binding.transferCodeContent.isVisible = false
			}
			is TransferCodeConversionState.CONVERTED -> {
				val dccHolder = state.dccHolder
				parentFragmentManager.popBackStack()

				// If the transfer code was converted to a certificate, open the certificate detail without an animation
				parentFragmentManager.beginTransaction()
					.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(dccHolder))
					.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
					.commit()
			}
			is TransferCodeConversionState.NOT_CONVERTED -> {
				binding.transferCodeLoadingIndicator.isVisible = false
				binding.transferCodeContent.isVisible = true

				transferCode = transferCode?.let {
					certificatesViewModel.updateTransferCodeLastUpdated(it)
				}
				setTransferCodeViewState(false)
			}
			is TransferCodeConversionState.ERROR -> {
				binding.transferCodeLoadingIndicator.isVisible = false
				binding.transferCodeContent.isVisible = true
				setTransferCodeViewState(false, state.error)
			}
		}
	}

	private fun onRefreshButtonClicked() {
		transferCode?.let {
			binding.transferCodeRefreshButton.rotate(360f, 300L, 0f)
			transferCodeViewModel.downloadCertificateForTransferCode(it, FORCE_RELOAD_DELAY)
		}
	}

	private fun onDeleteButtonClicked() {
		AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
			.setTitle(R.string.delete_button)
			.setMessage(R.string.wallet_transfer_delete_confirm_text)
			.setPositiveButton(R.string.delete_button) { _, _ ->
				transferCode?.let {
					transferCodeViewModel.removeTransferCode(it)
					certificatesViewModel.removeTransferCode(it)
					parentFragmentManager.popBackStack()
				}
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