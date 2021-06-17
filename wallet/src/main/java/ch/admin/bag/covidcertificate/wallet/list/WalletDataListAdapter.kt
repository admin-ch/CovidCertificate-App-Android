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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel

class WalletDataListAdapter(
	private val onCertificateClicked: ((DccHolder) -> Unit)? = null,
	private val onTransferCodeClicked: ((TransferCodeModel) -> Unit)? = null,
	private val onWalletItemMovedListener: ((from: Int, to: Int) -> Unit)? = null,
	private val onDragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null
) : RecyclerView.Adapter<WalletDataListViewHolder>() {

	private val items = mutableListOf<WalletDataListItem>()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletDataListViewHolder {
		return if (viewType == 1) {
			WalletDataListViewHolder.TransferCodeListViewHolder.inflate(
				LayoutInflater.from(parent.context),
				parent,
				onDragStartListener
			)
		} else {
			WalletDataListViewHolder.CertificatesListViewHolder.inflate(
				LayoutInflater.from(parent.context),
				parent,
				onDragStartListener
			)
		}
	}

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is WalletDataListItem.VerifiedCeritificateItem -> 0
			is WalletDataListItem.TransferCodeItem -> 1
		}
	}

	override fun onBindViewHolder(holder: WalletDataListViewHolder, position: Int) {
		when (holder) {
			is WalletDataListViewHolder.CertificatesListViewHolder -> holder.bindItem(
				items[position] as WalletDataListItem.VerifiedCeritificateItem,
				onCertificateClicked
			)
			is WalletDataListViewHolder.TransferCodeListViewHolder -> holder.bindItem(
				items[position] as WalletDataListItem.TransferCodeItem,
				onTransferCodeClicked
			)
		}
	}

	override fun getItemCount(): Int = items.size

	fun getItems() = items.toList()

	fun setItems(newItems: List<WalletDataListItem>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}

	fun itemMoved(from: Int, to: Int) {
		val certificate = items.removeAt(from)
		items.add(to, certificate)
		onWalletItemMovedListener?.invoke(from, to)
	}

}