package ch.admin.bag.covidcertificate.common.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt

fun View.showAnimated(
	duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong(),
	translationXby: Float = 0f,
	translationYby: Float = 0f,
	fade: Boolean = true
) {
	animation?.cancel()
	if (visibility == View.VISIBLE) return

	visibility = View.VISIBLE
	alpha = if (fade) 0f else 1f
	translationX = translationXby
	translationY = translationYby

	animate()
		.setDuration(duration)
		.alpha(1f)
		.translationX(0f)
		.translationY(0f)
		.setInterpolator(DecelerateInterpolator())
		.setListener(null)
}

fun View.hideAnimated(
	duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong(),
	translationXby: Float = 0f,
	translationYby: Float = 0f,
	fade: Boolean = true
) {
	animation?.cancel()
	animate()
		.setDuration((duration * alpha).toLong())
		.alpha(if (fade) 0f else 1f)
		.translationX(translationXby)
		.translationY(translationYby)
		.setInterpolator(AccelerateInterpolator())
		.setListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator?) {
				visibility = View.GONE
			}
		})
}

fun List<View>.animateBackgroundTintColor(
	@ColorInt targetColor: Int,
	duration: Long = get(0).resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
) {
	val startColor = get(0).backgroundTintList?.defaultColor ?: 0
	val colorAnim = ValueAnimator.ofArgb(startColor, targetColor).apply {
		this.duration = duration
		addUpdateListener { animator ->
			val backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
			this@animateBackgroundTintColor.forEach { it.backgroundTintList = backgroundTintList }
		}
	}
	colorAnim.start()
}

fun View.animateBackgroundTintColor(
	@ColorInt targetColor: Int,
	duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
) {
	val startColor = backgroundTintList?.defaultColor ?: 0
	val colorAnim = ValueAnimator.ofArgb(startColor, targetColor).apply {
		this.duration = duration
		addUpdateListener { animator ->
			backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
		}
	}
	colorAnim.start()
}