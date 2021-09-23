/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.views.hideAnimated
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificatesListBinding
import ch.admin.bag.covidcertificate.wallet.detail.CertificateDetailFragment
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.WalletItem
import ch.admin.bag.covidcertificate.wallet.light.CertificateLightDetailFragment
import ch.admin.bag.covidcertificate.wallet.transfercode.TransferCodeDetailFragment
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel

class CertificatesListFragment : Fragment() {

	companion object {
		fun newInstance(): CertificatesListFragment = CertificatesListFragment()
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private var _binding: FragmentCertificatesListBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentCertificatesListBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.certificatesOverviewToolbar.setNavigationOnClickListener { v: View? ->
			parentFragmentManager.popBackStack()
		}
		setupRecyclerView()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupRecyclerView() {
		val recyclerView = binding.certificatesOverviewRecyclerView

		val itemTouchHelper = CertificatesListTouchHelper()
		itemTouchHelper.attachToRecyclerView(recyclerView)

		val adapter = WalletDataListAdapter(
			onCertificateClicked = { openCertificateDetails(it.first, it.second) },
			onTransferCodeClicked = { openTransferCodeDetails(it) },
			onWalletItemMovedListener = { from, to -> certificatesViewModel.moveWalletDataItem(from, to) },
			onDragStartListener = { itemTouchHelper.startDrag(it) }
		)

		recyclerView.adapter = adapter

		binding.certificatesOverviewLoadingGroup.isVisible = true

		certificatesViewModel.walletItems.observe(viewLifecycleOwner) { walletItems ->
			if (walletItems.isEmpty()) {
				parentFragmentManager.popBackStack()
			}
			binding.certificatesOverviewLoadingGroup.hideAnimated()

			val adapterItems = walletItems.map {
				when (it) {
					is WalletItem.CertificateHolderItem -> WalletDataListItem.VerifiedCeritificateItem(
						StatefulWalletItem.VerifiedCertificate(it.qrCodeData, it.certificateHolder, VerificationState.LOADING),
						it.qrCodeImage
					)
					is WalletItem.TransferCodeHolderItem -> WalletDataListItem.TransferCodeItem(
						StatefulWalletItem.TransferCodeConversionItem(it.transferCode, TransferCodeConversionState.LOADING)
					)
				}
			}
			adapter.setItems(adapterItems)
		}

		certificatesViewModel.statefulWalletItems.observe(viewLifecycleOwner) { statefulWalletItems ->
			val adapterItems = adapter.getItems()
			val updatedAdapterItems = adapterItems.map { item ->
				when (item) {
					is WalletDataListItem.VerifiedCeritificateItem -> {
						statefulWalletItems.filterIsInstance(StatefulWalletItem.VerifiedCertificate::class.java)
							.find {
								it.qrCodeData == item.verifiedCertificate.qrCodeData
							}?.let {
								item.copy(verifiedCertificate = it)
							} ?: item
					}
					is WalletDataListItem.TransferCodeItem -> {
						statefulWalletItems.filterIsInstance(StatefulWalletItem.TransferCodeConversionItem::class.java)
							.find {
								it.transferCode == item.conversionItem.transferCode
							}?.let {
								item.copy(conversionItem = it)
							} ?: item
					}
				}
			}
			adapter.setItems(updatedAdapterItems)
		}

		certificatesViewModel.loadWalletData()
	}

	private fun openCertificateDetails(certificateHolder: CertificateHolder, qrCodeImage: String?) {
		if (certificateHolder.containsChLightCert() && qrCodeImage != null) {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateLightDetailFragment.newInstance(certificateHolder, qrCodeImage))
				.addToBackStack(CertificateLightDetailFragment::class.java.canonicalName)
				.commit()
		} else {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(certificateHolder))
				.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
				.commit()
		}
	}

	private fun openTransferCodeDetails(transferCode: TransferCodeModel) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, TransferCodeDetailFragment.newInstance(transferCode))
			.addToBackStack(TransferCodeDetailFragment::class.java.canonicalName)
			.commit()
	}

}