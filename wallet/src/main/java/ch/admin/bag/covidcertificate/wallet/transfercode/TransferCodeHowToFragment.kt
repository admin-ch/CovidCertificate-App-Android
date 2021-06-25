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
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.config.FaqModel
import ch.admin.bag.covidcertificate.common.faq.FaqAdapter
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.Question
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentTransferCodeHowtoBinding

class TransferCodeHowToFragment : Fragment(R.layout.fragment_transfer_code_howto) {

	companion object {
		fun newInstance() = TransferCodeHowToFragment()
	}

	private var _binding: FragmentTransferCodeHowtoBinding? = null
	private val binding get() = _binding!!

	private val configViewModel by activityViewModels<ConfigViewModel>()
	private val faqAdapter = FaqAdapter { url: String -> context?.let { UrlUtil.openUrl(it, url) } }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentTransferCodeHowtoBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		binding.transferCodeHowtoList.adapter = faqAdapter

		configViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			val languageKey = getString(R.string.language_key)
			config.getTransferQuestionsFaqs(languageKey)?.let {
				showTransferCodeFaqItems(it)
			}
		}
		configViewModel.loadConfig(BuildConfig.BASE_URL, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME.toString())
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun showTransferCodeFaqItems(faqModel: FaqModel) {
		val header = listOf(Header(faqModel.faqIconAndroid, faqModel.faqTitle, faqModel.faqSubTitle))
		val questions = faqModel.faqEntries?.map { Question(it.title, it.text, false, it.linkTitle, it.linkUrl) } ?: emptyList()
		val adapterItems: List<Faq> = listOf(header, questions).flatten()
		faqAdapter.setItems(adapterItems)
	}
}