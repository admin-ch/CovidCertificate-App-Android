/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.faq

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.IntroSection
import ch.admin.bag.covidcertificate.common.faq.model.Question

class FaqAdapter(val onLinkClickListener: ((String) -> Unit)? = null) : RecyclerView.Adapter<FaqViewHolder>() {

	private val items = mutableListOf<FaqItem>()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			0 -> FaqViewHolder(inflater.inflate(HeaderItem.layoutResource, parent, false))
			1 -> FaqViewHolder(inflater.inflate(QuestionItem.layoutResource, parent, false))
			2 -> FaqViewHolder(inflater.inflate(IntroSectionItem.layoutResource, parent, false))
			else -> throw IllegalStateException("Unknown viewType $viewType in FaqAdapter")
		}
	}

	override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
		holder.bindItem(items[position]) { notifyItemChanged(position) }
	}

	override fun getItemCount(): Int = items.size

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is HeaderItem -> 0
			is QuestionItem -> 1
			is IntroSectionItem -> 2
		}
	}

	fun setItems(items: List<Faq>) {
		this.items.clear()
		val newItems = items.mapNotNull {
			when (it) {
				is Header -> HeaderItem(it)
				is Question -> QuestionItem(it, onLinkClickListener)
				is IntroSection -> IntroSectionItem(it)
				else -> null
			}
		}
		this.items.addAll(newItems)
		notifyDataSetChanged()
	}

}