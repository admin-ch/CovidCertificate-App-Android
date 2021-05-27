package ch.admin.bag.covidcertificate.wallet.detail

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CertificateDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	fun bindItem(item: CertificateDetailItem) = item.bindView(itemView)
}