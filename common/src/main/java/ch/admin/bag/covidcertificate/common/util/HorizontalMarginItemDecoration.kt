package ch.admin.bag.covidcertificate.common.util

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalMarginItemDecoration(context: Context, val horizontalMarginInPx: Int) :
		RecyclerView.ItemDecoration() {

		override fun getItemOffsets(
			outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
		) {
			outRect.right = horizontalMarginInPx
			outRect.left = horizontalMarginInPx
		}

	}