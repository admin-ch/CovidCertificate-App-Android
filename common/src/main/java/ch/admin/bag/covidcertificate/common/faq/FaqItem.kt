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

import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.databinding.ItemFaqHeaderBinding
import ch.admin.bag.covidcertificate.common.databinding.ItemFaqIntroSectionBinding
import ch.admin.bag.covidcertificate.common.databinding.ItemFaqQuestionBinding
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.IntroSection
import ch.admin.bag.covidcertificate.common.faq.model.Question

sealed class FaqItem {
	abstract fun bindView(view: View, onItemClickListener: (() -> Unit)? = null)
}

data class HeaderItem(val header: Header) : FaqItem() {
	companion object {
		val layoutResource = R.layout.item_faq_header
	}

	override fun bindView(view: View, onItemClickListener: (() -> Unit)?) {
		val binding = ItemFaqHeaderBinding.bind(view)
		binding.itemFaqHeaderTitle.text = header.title
		binding.itemFaqHeaderText.apply {
			text = header.subtitle
			isVisible = header.subtitle != null
		}

		val drawableId = header.iconName?.let { iconName ->
			view.context.resources.getIdentifier(iconName, "drawable", view.context.packageName)
		} ?: 0

		binding.itemFaqHeaderIllu.apply {
			setImageResource(drawableId)
			isVisible = drawableId != 0
		}
	}
}

data class QuestionItem(
	val question: Question,
	val onLinkClickListener: OnUrlClickListener? = null,
) : FaqItem() {
	companion object {
		val layoutResource = R.layout.item_faq_question
	}

	override fun bindView(view: View, onItemClickListener: (() -> Unit)?) {
		val binding = ItemFaqQuestionBinding.bind(view)

		binding.root.setOnClickListener {
			question.isSelected = !question.isSelected
			view.doOnPreDraw { onItemClickListener?.invoke() }
		}
		binding.itemFaqQuestionTitle.text = question.question
		binding.itemFaqQuestionAnswer.apply {
			text = question.answer
			isVisible = question.isSelected
		}
		val hasLink = !question.linkTitle.isNullOrEmpty() && !question.linkUrl.isNullOrEmpty()
		(hasLink && question.isSelected).let { visible ->
			binding.itemFaqQuestionLinkLabel.isVisible = visible
			binding.itemFaqQuestionLink.isVisible = visible
		}

		if (hasLink) {
			binding.itemFaqQuestionLinkLabel.text = question.linkTitle
			binding.itemFaqQuestionLink.setOnClickListener { onLinkClickListener?.onLinkClicked(question.linkUrl!!) }
		} else {
			binding.itemFaqQuestionLink.setOnClickListener(null)
		}

		binding.itemFaqQuestionChevron.setImageResource(if (question.isSelected) R.drawable.ic_arrow_contract else R.drawable.ic_arrow_expand)
	}
}

data class IntroSectionItem(val introSection: IntroSection) : FaqItem() {
	companion object {
		val layoutResource = R.layout.item_faq_intro_section
	}

	override fun bindView(view: View, onItemClickListener: (() -> Unit)?) {
		val binding = ItemFaqIntroSectionBinding.bind(view)
		val drawableId = view.context.resources.getIdentifier(introSection.iconName, "drawable", view.context.packageName)
		binding.faqIntroSectionIcon.setImageResource(drawableId)
		binding.faqIntroSectionText.text = introSection.text
	}
}
