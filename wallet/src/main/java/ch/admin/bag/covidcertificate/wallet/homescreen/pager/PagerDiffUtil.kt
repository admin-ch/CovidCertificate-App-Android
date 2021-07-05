/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.homescreen.pager

import androidx.recyclerview.widget.DiffUtil

class PagerDiffUtil(private val oldList: List<WalletItem>, private val newList: List<WalletItem>) : DiffUtil.Callback() {

	enum class PayloadKey {
		VALUE
	}

	override fun getOldListSize() = oldList.size

	override fun getNewListSize() = newList.size

	override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
		return oldList[oldItemPosition].id == newList[newItemPosition].id
	}

	override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
		val oldItem = oldList[oldItemPosition]
		val newItem = newList[newItemPosition]

		val isSameDccContent = oldItem is WalletItem.DccHolderItem
				&& newItem is WalletItem.DccHolderItem
				&& oldItem.qrCodeData == newItem.qrCodeData
				&& oldItem.certificateHolder?.qrCodeData == newItem.certificateHolder?.qrCodeData

		val isSameTransferCodeContent = oldItem is WalletItem.TransferCodeHolderItem
				&& newItem is WalletItem.TransferCodeHolderItem
				&& oldItem.transferCode == newItem.transferCode

		return isSameDccContent || isSameTransferCodeContent
	}

	override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
		return listOf(PayloadKey.VALUE)
	}
}