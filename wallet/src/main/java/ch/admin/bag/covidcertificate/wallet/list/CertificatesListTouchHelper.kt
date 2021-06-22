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

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.wallet.R

class CertificatesListTouchHelper : ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {

	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		val adapter = recyclerView.adapter as WalletDataListAdapter
		val from = viewHolder.adapterPosition
		val to = target.adapterPosition

		adapter.notifyItemMoved(from, to)
		adapter.itemMoved(from, to)

		return true
	}

	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
		// not used
	}

	override fun onChildDraw(
		c: Canvas,
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		dX: Float,
		dY: Float,
		actionState: Int,
		isCurrentlyActive: Boolean
	) {
		super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
		if (isCurrentlyActive) {
			val itemView = viewHolder.itemView
			itemView.elevation = itemView.context.resources.getDimension(R.dimen.default_elevation)
		}
	}

	override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
		super.clearView(recyclerView, viewHolder)
		viewHolder.itemView.elevation = 0f
	}
})