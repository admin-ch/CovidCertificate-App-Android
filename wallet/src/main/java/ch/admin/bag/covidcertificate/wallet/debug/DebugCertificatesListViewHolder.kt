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
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.R

class DebugCertificatesListViewHolder(
	itemView: View,
) : RecyclerView.ViewHolder(itemView) {

	companion object {
		fun inflate(
			inflater: LayoutInflater,
			parent: ViewGroup,
		): DebugCertificatesListViewHolder {
			val itemView = inflater.inflate(R.layout.item_debug_certificate_list, parent, false)
			return DebugCertificatesListViewHolder(itemView)
		}
	}

	fun bindItem(certItem: DebugCertificateItem, onCopyClickListener: ((DccHolder) -> Unit)) =
		certItem.bindView(itemView, onCopyClickListener)
}