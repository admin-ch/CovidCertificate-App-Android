package ch.admin.bag.covidcertificate.common.faq

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.Question


class FaqAdapter(val onItemClickListener: OnUrlClickListener? = null) : RecyclerView.Adapter<FaqViewHolder>() {

	private val items = mutableListOf<FaqItem>()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			0 -> FaqViewHolder(inflater.inflate(HeaderItem.layoutResource, parent, false))
			1 -> FaqViewHolder(inflater.inflate(QuestionItem.layoutResource, parent, false))
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
		}
	}

	fun setItems(items: List<Faq>) {
		this.items.clear()
		for (item in items) {
			if (item is Header) {
				this.items.add(HeaderItem(item))
			} else if (item is Question) {
				this.items.add(QuestionItem(item, onLinkClickListener = onItemClickListener))
			}
		}
		notifyDataSetChanged()
	}

}

interface OnUrlClickListener {
	fun onLinkClicked(url: String)
}