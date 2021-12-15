package ch.admin.bag.covidcertificate.verifier.verification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.verifier.databinding.ItemVerificationStatusBinding
import ch.admin.bag.covidcertificate.verifier.databinding.ItemVerificationStatusInfoBinding

class VerificationAdapter(
	private val onRetryClickListener: View.OnClickListener,
	private val onPlayStoreClickListener: View.OnClickListener
) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val items: MutableList<VerificationItem> = mutableListOf()

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is StatusItem -> 0
			is InfoItem -> 1
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			0 -> StatusViewHolder(ItemVerificationStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false))
			else -> InfoViewHolder(
				ItemVerificationStatusInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
				onRetryClickListener,
				onPlayStoreClickListener
			)
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		val type = getItemViewType(position)
		when (type) {
			0 -> (holder as StatusViewHolder).bindStatusItem(item as StatusItem)
			else -> (holder as InfoViewHolder).bindStatusItem(item as InfoItem)
		}
	}

	override fun getItemCount(): Int = items.size

	fun setItems(newItems: List<VerificationItem>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}

}