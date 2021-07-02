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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentDebugBinding

class DebugFragment : Fragment() {

	companion object {
		fun newInstance(): DebugFragment = DebugFragment()

		const val EXISTS = BuildConfig.FLAVOR == "abn" || BuildConfig.FLAVOR == "dev"
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentDebugBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentDebugBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		// Have additional self-control to not be shown in PROD
		if (!EXISTS) {
			parentFragmentManager.popBackStack()
		}

		binding.toolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}
		setupRecyclerView()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupRecyclerView() {
		val recyclerView = binding.certificatesRecyclerView

		val adapter = DebugCertificatesListAdapter(::onShareClickListener)
		recyclerView.adapter = adapter

		certificatesViewModel.verifiedCertificates.observe(viewLifecycleOwner) { verifiedCertificates ->
			adapter.setItems(verifiedCertificates.map { DebugCertificateItem(it) })
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