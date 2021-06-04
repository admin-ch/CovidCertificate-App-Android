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

class CertificatesListAdapter(
	private val onCertificateClickListener: ((DccHolder) -> Unit)? = null,
	private val onCertificateMovedListener: ((from: Int, to: Int) -> Unit)? = null,
	private val onDragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null
) :
	RecyclerView.Adapter<CertificatesListViewHolder>() {

	private val items: MutableList<VerifiedCeritificateItem> = mutableListOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificatesListViewHolder {
		return CertificatesListViewHolder.inflate(LayoutInflater.from(parent.context), parent, onDragStartListener)
	}

	override fun onBindViewHolder(holder: CertificatesListViewHolder, position: Int) {
		holder.bindItem(items[position], onCertificateClickListener)
	}

	override fun getItemCount(): Int = items.size

	fun setItems(items: List<VerifiedCeritificateItem>) {
		this.items.clear()
		this.items.addAll(items)
		notifyDataSetChanged()
	}

	fun itemMoved(from: Int, to: Int) {
		val certificate = items.removeAt(from)
		items.add(to, certificate)
		onCertificateMovedListener?.invoke(from, to)
	}

}