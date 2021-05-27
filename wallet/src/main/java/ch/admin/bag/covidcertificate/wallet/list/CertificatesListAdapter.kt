package ch.admin.bag.covidcertificate.wallet.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.eval.models.Bagdgc

class CertificatesListAdapter(
	private val onCertificateClickListener: ((Bagdgc) -> Unit)? = null,
	private val onCertificateMovedListener: ((from: Int, to: Int) -> Unit)? = null,
	private val onDragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null
) :
	RecyclerView.Adapter<CertificatesListViewHolder>() {

	private val items: MutableList<VerifiedCeritificateItem> = mutableListOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificatesListViewHolder {
		return CertificatesListViewHolder.inflate(LayoutInflater.from(parent.context), parent, onDragStartListener)
	}

	override fun onBindViewHolder(holder: CertificatesListViewHolder, position: Int) {
		holder.bindItem(items[position], onCertificateClickListener)
	}

	override fun getItemCount(): Int = items.size

	fun setItems(items: List<VerifiedCeritificateItem>) {
		this.items.clear()
		this.items.addAll(items)
		notifyDataSetChanged()
	}

	fun itemMoved(from: Int, to: Int) {
		val certificate = items.removeAt(from)
		items.add(to, certificate)
		onCertificateMovedListener?.invoke(from, to)
	}

}