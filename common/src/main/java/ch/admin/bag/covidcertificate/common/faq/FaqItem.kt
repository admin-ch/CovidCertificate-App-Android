package ch.admin.bag.covidcertificate.common.faq

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.Question

sealed class FaqItem {
	abstract fun bindView(view: View, onItemClickListener: (() -> Unit)? = null)
}

data class HeaderItem(val header: Header) : FaqItem() {
	companion object {
		val layoutResource = R.layout.item_faq_header
	}

	override fun bindView(view: View, onItemClickListener: (() -> Unit)?) {
		view.findViewById<TextView>(R.id.item_faq_header_title).text = header.title
		view.findViewById<TextView>(R.id.item_faq_header_text).apply {
			text = header.subtitle
			isVisible = header.subtitle != null
		}
		val drawableId = header.iconName?.let { iconName ->
			view.context.resources.getIdentifier(iconName, "drawable", view.context.packageName)
		} ?: 0

		view.findViewById<ImageView>(R.id.item_faq_header_illu).apply {
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
		view.setOnClickListener {
			question.isSelected = !question.isSelected
			view.doOnPreDraw { onItemClickListener?.invoke() }
		}
		view.findViewById<TextView>(R.id.item_faq_question_title).text = question.question
		view.findViewById<TextView>(R.id.item_faq_question_answer).apply {
			text = question.answer
			isVisible = question.isSelected
		}
		view.findViewById<TextView>(R.id.item_faq_question_link).apply {
			val hasLink = !question.linkTitle.isNullOrEmpty() && !question.linkUrl.isNullOrEmpty()
			isVisible = hasLink && question.isSelected
			if (hasLink) {
				text = question.linkTitle
				setOnClickListener { onLinkClickListener?.onLinkClicked(question.linkUrl!!) }
			}
		}
		view.findViewById<ImageView>(R.id.item_faq_question_chevron)
			.setImageResource(if (question.isSelected) R.drawable.ic_arrow_contract else R.drawable.ic_arrow_expand)
	}
}
