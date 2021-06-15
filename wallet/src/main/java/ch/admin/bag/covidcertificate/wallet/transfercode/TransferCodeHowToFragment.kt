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
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.faq.FaqAdapter
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.Question
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodeHowtoBinding

class TransferCodeHowToFragment : Fragment(R.layout.fragment_transfer_code_howto) {

	companion object {
		fun newInstance() = TransferCodeHowToFragment()
	}

	private var _binding: FragmentTransferCodeHowtoBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentTransferCodeHowtoBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		val faqAdapter = FaqAdapter()
		faqAdapter.setItems(
			listOf(
				Header(
					"illu_transfer_code_howto",
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_title),
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_subtitle)
				),
				Question(
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_question_1),
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_answer_1)
				),
				Question(
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_question_2),
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_answer_2)
				),
				Question(
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_question_3),
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_answer_3)
				),
				Question(
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_question_4),
					requireContext().getString(R.string.wallet_transfer_code_faq_questions_answer_4)
				)
			)
		)

		binding.transferCodeHowtoList.adapter = faqAdapter
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}