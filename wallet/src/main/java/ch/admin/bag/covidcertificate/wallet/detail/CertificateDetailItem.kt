package ch.admin.bag.covidcertificate.wallet.detail

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import ch.admin.bag.covidcertificate.wallet.R

sealed class CertificateDetailItem {
	abstract fun bindView(view: View)
}

data class TitleItem(@StringRes val titleResource: Int) : CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_title
	}

	override fun bindView(view: View) {
		view.findViewById<TextView>(R.id.item_title).setText(titleResource)
	}
}

data class TitleStatusItem(@StringRes val labelResource: Int, val status: String) : CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_title_status
	}

	override fun bindView(view: View) {
		view.findViewById<TextView>(R.id.item_title_status_label).setText(labelResource)
		view.findViewById<TextView>(R.id.item_title_status_value).text = status
	}
}

data class ValueItem(@StringRes val labelResource: Int, val value: String) : CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_value
	}

	override fun bindView(view: View) {
		view.findViewById<TextView>(R.id.item_value_label).setText(labelResource)
		view.findViewById<TextView>(R.id.item_value_value).text = value
	}
}

data class ValueItemWithoutLabel(val value: String) : CertificateDetailItem() {
	companion object {
		const val layoutResource = R.layout.item_detail_value_without_label
	}

	override fun bindView(view: View) {
		view.findViewById<TextView>(R.id.item_value_text).text = value
	}
}

object DividerItem : CertificateDetailItem() {
	const val layoutResource = R.layout.item_detail_divider
	override fun bindView(view: View) {}
}
