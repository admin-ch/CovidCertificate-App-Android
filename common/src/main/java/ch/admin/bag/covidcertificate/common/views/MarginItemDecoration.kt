package ch.admin.bag.covidcertificate.common.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView

class VerticalMarginItemDecoration(context: Context, @DimenRes marginRes: Int) : RecyclerView.ItemDecoration() {

	private val marginPx = context.resources.getDimensionPixelSize(marginRes)

	override fun getItemOffsets(
		outRect: Rect, view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		with(outRect) {
			if (parent.getChildAdapterPosition(view) != 0) {
				top = marginPx
			}
		}
	}
}