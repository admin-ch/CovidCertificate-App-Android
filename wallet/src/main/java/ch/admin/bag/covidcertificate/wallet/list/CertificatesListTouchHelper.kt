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

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class CertificatesListTouchHelper :
	ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
		var movedFromTo: Pair<Int, Int>? = null

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			val adapter = recyclerView.adapter as CertificatesListAdapter
			val from = viewHolder.adapterPosition
			val to = target.adapterPosition

			adapter.notifyItemMoved(from, to)
			if (movedFromTo == null) {
				movedFromTo = Pair(from, to)
			} else {
				movedFromTo?.let { movedFromTo = Pair(it.first, to) }
			}
			return true
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			// not used
		}

		override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
			super.clearView(recyclerView, viewHolder)
			movedFromTo?.let {
				val adapter = recyclerView.adapter as CertificatesListAdapter
				adapter.itemMoved(it.first, it.second)
			}
			movedFromTo = null
		}
	})