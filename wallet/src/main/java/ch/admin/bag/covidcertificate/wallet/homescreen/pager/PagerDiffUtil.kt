package ch.admin.bag.covidcertificate.wallet.homescreen.pager

import androidx.recyclerview.widget.DiffUtil

class PagerDiffUtil(private val oldList: List<BagdgcItem>, private val newList: List<BagdgcItem>) : DiffUtil.Callback() {

	enum class PayloadKey {
		VALUE
	}

	override fun getOldListSize() = oldList.size

	override fun getNewListSize() = newList.size

	override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
		return oldList[oldItemPosition].id == newList[newItemPosition].id
	}

	override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
		return oldList[oldItemPosition].bagdgc.qrCodeData == newList[newItemPosition].bagdgc.qrCodeData
	}

	override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
		return listOf(PayloadKey.VALUE)
	}
}