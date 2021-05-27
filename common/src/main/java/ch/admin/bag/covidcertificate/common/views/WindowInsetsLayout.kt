package ch.admin.bag.covidcertificate.common.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.view.children
import ch.admin.bag.covidcertificate.common.R

/**
 * A FrameLayout that selectively applies the system window insets to itself.
 *
 * `applyWindowInsets` defines on which edges the insets are applied to this layout.
 * Can be one or more of `left`, `top`, `right` or `bottom`,
 * or `all` to include all four edges; default is `none`.
 *
 * Insets that have not been consumed are passed down to all child views
 * (independent of each other, not just the first one that consumes it,
 * compared to the standard behaviour of `fitsSystemWindows`).
 */
class WindowInsetsLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private var applyInsetEdges: Int

	init {
		val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.WindowInsetsLayout, 0, 0)
		applyInsetEdges = typedArray.getInteger(R.styleable.WindowInsetsLayout_applyWindowInsets, INSETS_NOWHERE)
		typedArray.recycle()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		requestApplyInsets()
	}

	override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
		val appliedInset = Rect(
			if (applyInsetEdges and INSETS_LEFT != 0) insets.systemWindowInsetLeft else 0,
			if (applyInsetEdges and INSETS_TOP != 0) insets.systemWindowInsetTop else 0,
			if (applyInsetEdges and INSETS_RIGHT != 0) insets.systemWindowInsetRight else 0,
			if (applyInsetEdges and INSETS_BOTTOM != 0) insets.systemWindowInsetBottom else 0
		)
		setPadding(appliedInset.left, appliedInset.top, appliedInset.right, appliedInset.bottom)
		if (applyInsetEdges == INSETS_EVERYWHERE) {
			return insets
		}
		val remainingInsets = insets.replaceSystemWindowInsets(
			Rect(
				insets.systemWindowInsetLeft - appliedInset.left,
				insets.systemWindowInsetTop - appliedInset.top,
				insets.systemWindowInsetRight - appliedInset.right,
				insets.systemWindowInsetBottom - appliedInset.bottom
			)
		)
		children.forEach {
			it.dispatchApplyWindowInsets(remainingInsets)
		}
		return insets
	}

	fun setInsets(insetEdges: Int) {
		applyInsetEdges = insetEdges
		requestApplyInsets()
	}

	companion object {
		const val INSETS_NOWHERE = 0x0
		const val INSETS_EVERYWHERE = 0xF
		const val INSETS_LEFT = 0x1
		const val INSETS_TOP = 0x2
		const val INSETS_RIGHT = 0x4
		const val INSETS_BOTTOM = 0x8
	}
}