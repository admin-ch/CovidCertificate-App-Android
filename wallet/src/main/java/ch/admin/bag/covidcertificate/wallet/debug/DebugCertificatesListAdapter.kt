/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.debug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.list.VerifiedCeritificateItem

class DebugCertificatesListAdapter(
	private val onShareClickListener: ((DccHolder) -> Unit),
) :
	RecyclerView.Adapter<DebugCertificatesListViewHolder>() {

	private val items: MutableList<DebugCertificateItem> = mutableListOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugCertificatesListViewHolder {
		return DebugCertificatesListViewHolder.inflate(LayoutInflater.from(parent.context), parent)
	}

	override fun onBindViewHolder(holder: DebugCertificatesListViewHolder, position: Int) {
		holder.bindItem(items[position], onShareClickListener)
	}

	override fun getItemCount(): Int = items.size

	fun setItems(items: List<DebugCertificateItem>) {
		this.items.clear()
		this.items.addAll(items)
		notifyDataSetChanged()
	}
}