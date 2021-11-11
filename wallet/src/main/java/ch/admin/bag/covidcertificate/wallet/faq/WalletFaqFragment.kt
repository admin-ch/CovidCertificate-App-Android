/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.faq

import android.os.Bundle
import ch.admin.bag.covidcertificate.common.faq.FaqFragment
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.getInstanceWallet

class WalletFaqFragment : FaqFragment() {

	companion object {
		fun newInstance(): WalletFaqFragment = WalletFaqFragment()
	}

	private lateinit var configRepository: ConfigRepository

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		configRepository = ConfigRepository.getInstanceWallet(requireContext())
	}

	override fun setupFaqProvider() {
		toolbar.setTitle(R.string.wallet_faq_header)
		configRepository.configLiveData.observe(viewLifecycleOwner, { config ->
			val languageKey = getString(R.string.language_key)
			setupFaqList(config.generateFaqItems(languageKey))
		})
		configRepository.loadConfig()
	}

}