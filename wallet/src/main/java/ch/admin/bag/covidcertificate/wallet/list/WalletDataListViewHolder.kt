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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel

sealed class WalletDataListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

	class CertificatesListViewHolder(itemView: View) : WalletDataListViewHolder(itemView) {

		companion object {
			@SuppressLint("ClickableViewAccessibility")
			fun inflate(
				inflater: LayoutInflater,
				parent: ViewGroup,
				onStartDragListener: ((CertificatesListViewHolder) -> Unit)? = null
			): CertificatesListViewHolder {
				val itemView = inflater.inflate(R.layout.item_certificate_list, parent, false)
				val viewHolder = CertificatesListViewHolder(itemView)
				itemView.findViewById<View>(R.id.item_certificate_list_drag_handle).setOnTouchListener { _, event ->
					if (event.actionMasked == MotionEvent.ACTION_DOWN) {
						onStartDragListener?.invoke(viewHolder)
					}
					return@setOnTouchListener false
				}
				return viewHolder
			}
		}

		fun bindItem(
			item: WalletDataListItem.VerifiedCeritificateItem,
			onCertificateClickListener: ((DccHolder) -> Unit)? = null
		) = item.bindView(itemView, onCertificateClickListener)
	}

	class TransferCodeListViewHolder(itemView: View) : WalletDataListViewHolder(itemView) {

		companion object {
			@SuppressLint("ClickableViewAccessibility")
			fun inflate(
				inflater: LayoutInflater,
				parent: ViewGroup,
				onStartDragListener: ((TransferCodeListViewHolder) -> Unit)? = null
			): TransferCodeListViewHolder {
				val itemView = inflater.inflate(R.layout.item_transfer_code_list, parent, false)
				val viewHolder = TransferCodeListViewHolder(itemView)
				itemView.findViewById<View>(R.id.item_transfer_code_list_drag_handle).setOnTouchListener { _, event ->
					if (event.actionMasked == MotionEvent.ACTION_DOWN) {
						onStartDragListener?.invoke(viewHolder)
					}
					return@setOnTouchListener false
				}
				return viewHolder
			}
		}

		fun bindItem(
			item: WalletDataListItem.TransferCodeItem,
			onTransferCodeClickListener: ((TransferCodeModel) -> Unit)? = null
		) = item.bindView(itemView, onTransferCodeClickListener)
	}
}