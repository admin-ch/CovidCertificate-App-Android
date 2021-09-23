package ch.admin.bag.covidcertificate.wallet.debug
/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.debug.DebugFragment
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem

class WalletDebugFragment : DebugFragment() {

	companion object {
		fun newInstance(): DebugFragment = DebugFragment()
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setupRecyclerView()
	}

	private fun setupRecyclerView() {
		val recyclerView = binding.certificatesRecyclerView

		val adapter = DebugCertificatesListAdapter(::onShareClickListener)
		recyclerView.adapter = adapter

		certificatesViewModel.statefulWalletItems.observe(viewLifecycleOwner) { items ->
			val adapterItems = items
				.filterIsInstance(StatefulWalletItem.VerifiedCertificate::class.java)
				.map { DebugCertificateItem(it) }
			adapter.setItems(adapterItems)
		}

		certificatesViewModel.loadWalletData()
	}

	private fun onShareClickListener(qrCodeData: String) {
		val sendIntent = Intent().apply {
			action = Intent.ACTION_SEND
			putExtra(Intent.EXTRA_TEXT, qrCodeData)
			type = "text/plain"
		}
		val shareIntent = Intent.createChooser(sendIntent, null)
		startActivity(shareIntent)
	}
}