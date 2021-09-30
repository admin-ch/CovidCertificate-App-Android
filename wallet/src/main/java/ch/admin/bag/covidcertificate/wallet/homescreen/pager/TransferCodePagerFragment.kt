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
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
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
import ch.admin.bag.covidcertificate.wallet.vaccination.appointment.VaccinationAppointmentFragment
import ch.admin.bag.covidcertificate.wallet.vaccination.hint.VaccinationHintViewModel

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
	private val vaccinationHintViewModel by activityViewModels<VaccinationHintViewModel>()
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

		vaccinationHintViewModel.displayVaccinationHint.observe(viewLifecycleOwner) { shouldDisplayVaccinationHint ->
			displayVaccinationHint(shouldDisplayVaccinationHint)
		}
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

				if (error == null || error.code == ErrorCodes.GENERAL_OFFLINE) {
					binding.transferCodePageWaitingImage.isVisible = true
					binding.transferCodePageImage.isVisible = false
					binding.transferCodePageStatusLabel.text =
						requireContext().getString(R.string.wallet_transfer_code_state_waiting)
					binding.transferCodePageBubble.setState(TransferCodeBubbleView.TransferCodeBubbleState.Valid(isRefreshing,
						error))
				} else {
					binding.transferCodePageWaitingImage.isVisible = false
					binding.transferCodePageImage.isVisible = true
					binding.transferCodePageImage.setImageResource(R.drawable.illu_transfer_code_failed)
					binding.transferCodePageStatusLabel.text =
						requireContext().getString(R.string.wallet_transfer_code_state_expired)
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

	private fun displayVaccinationHint(display: Boolean) {
		binding.root.post {
			val shouldShowHintAndImage = isAvailableSpaceEnoughForHintAndImage()

			val vaccinationHint = ConfigRepository.getCurrentConfig(requireContext())
				?.getVaccinationHints(getString(R.string.language_key))
				?.randomOrNull()

			TransitionManager.beginDelayedTransition(binding.root)
			binding.transferCodePageVaccinationHint.isVisible = display && vaccinationHint != null
			binding.transferCodePageWaitingImage.isVisible = display.not() || shouldShowHintAndImage
			binding.vaccinationHintTitle.text = vaccinationHint?.title
			binding.vaccinationHintText.text = vaccinationHint?.text

			binding.vaccinationHintDismiss.setOnClickListener {
				vaccinationHintViewModel.dismissVaccinationHint()
			}

			binding.vaccinationHintBookNow.setOnClickListener {
				requireParentFragment().parentFragmentManager.beginTransaction()
					.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
					.replace(R.id.fragment_container, VaccinationAppointmentFragment.newInstance())
					.addToBackStack(VaccinationAppointmentFragment::class.java.canonicalName)
					.commit()
			}
		}
	}

	private fun isAvailableSpaceEnoughForHintAndImage(): Boolean {
		binding.apply {
			val fullHeight = root.height - root.paddingTop - root.paddingBottom
			val statusLabelHeight = transferCodePageStatusLabel.height + transferCodePageStatusLabel.marginTop + transferCodePageStatusLabel.marginBottom
			val statusBubbleHeight = transferCodePageBubble.height + transferCodePageBubble.marginTop + transferCodePageBubble.marginBottom

			val availableHeight = fullHeight - statusLabelHeight - statusBubbleHeight

			// In order to show both the vaccination hint and the waiting image, the available space should be at least half of the
			// entire height and also more than twice the status bubble
			return availableHeight >= fullHeight / 2 && availableHeight >= statusBubbleHeight * 2
		}
	}

}