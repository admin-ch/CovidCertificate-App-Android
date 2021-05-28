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
import ch.admin.bag.covidcertificate.eval.models.Bagdgc

class CertificatesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

	private var items: MutableList<BagdgcItem> = mutableListOf()

	override fun getItemCount(): Int = items.size

	override fun createFragment(position: Int): Fragment = CertificatePagerFragment.newInstance(items[position].bagdgc)

	override fun getItemId(position: Int): Long {
		return items[position].id.toLong()
	}

	override fun containsItem(itemId: Long): Boolean {
		return items.any { it.id.toLong() == itemId }
	}

	fun setData(data: List<Bagdgc>) {
		val newItems: ArrayList<BagdgcItem> = arrayListOf()

		for (i in data.indices) {
			newItems.add(BagdgcItem(data[i].dgc.hashCode(), data[i]))
		}

		val callback = PagerDiffUtil(items, newItems)
		val diff = DiffUtil.calculateDiff(callback)
		items.clear()
		items.addAll(newItems)
		diff.dispatchUpdatesTo(this)
	}

}

data class BagdgcItem(val id: Int, val bagdgc: Bagdgc)