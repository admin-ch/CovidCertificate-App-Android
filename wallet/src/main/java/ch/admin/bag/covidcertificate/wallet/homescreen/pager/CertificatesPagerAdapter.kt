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

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter

class CertificatesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

	private var items: MutableList<WalletItem> = mutableListOf()

	override fun getItemCount(): Int = items.size

	override fun createFragment(position: Int): Fragment = when (val item = items[position]) {
		is WalletItem.DccHolderItem -> CertificatePagerFragment.newInstance(item.qrCodeData, item.dccHolder)
		is WalletItem.TransferCodeHolderItem -> TransferCodePagerFragment.newInstance(item.transferCode)
	}

	override fun getItemId(position: Int): Long {
		return items[position].id.toLong()
	}

	override fun containsItem(itemId: Long): Boolean {
		return items.any { it.id.toLong() == itemId }
	}

	fun setData(newItems: List<WalletItem>) {
		val callback = PagerDiffUtil(items, newItems)
		val diff = DiffUtil.calculateDiff(callback)
		items.clear()
		items.addAll(newItems)
		diff.dispatchUpdatesTo(this)
	}

}
