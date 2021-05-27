package ch.admin.bag.covidcertificate.common.faq

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	fun bindItem(item: FaqItem, onItemClickListener: (() -> Unit)? = null) = item.bindView(itemView, onItemClickListener)
}