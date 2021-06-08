/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CertificateDetailAdapter : RecyclerView.Adapter<CertificateDetailViewHolder>() {

	private val items = mutableListOf<CertificateDetailItem>()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificateDetailViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			0 -> CertificateDetailViewHolder(inflater.inflate(DividerItem.layoutResource, parent, false))
			1 -> CertificateDetailViewHolder(inflater.inflate(TitleItem.layoutResource, parent, false))
			2 -> CertificateDetailViewHolder(inflater.inflate(ValueItem.layoutResource, parent, false))
			3 -> CertificateDetailViewHolder(inflater.inflate(ValueItemWithoutLabel.layoutResource, parent, false))
			else -> throw IllegalStateException("Unknown viewType $viewType in CertificateDetailAdapter")
		}
	}

	override fun onBindViewHolder(holder: CertificateDetailViewHolder, position: Int) {
		holder.bindItem(items[position])
	}

	override fun getItemCount(): Int = items.size

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			DividerItem -> 0
			is TitleItem -> 1
			is ValueItem -> 2
			is ValueItemWithoutLabel -> 3
		}
	}

	fun setItems(items: List<CertificateDetailItem>) {
		this.items.clear()
		this.items.addAll(items)
		notifyDataSetChanged()
	}
}