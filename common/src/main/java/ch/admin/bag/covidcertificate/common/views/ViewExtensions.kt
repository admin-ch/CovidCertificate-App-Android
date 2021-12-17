/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.util.CutOutEdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

fun View.showAnimated(
	duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong(),
	fade: Boolean = true
) {
	animation?.cancel()
	if (visibility == View.VISIBLE) return

	visibility = View.VISIBLE
	alpha = if (fade) 0f else 1f

	animate()
		.setDuration(duration)
		.alpha(1f)
		.setInterpolator(DecelerateInterpolator())
		.setListener(null)
}

fun View.hideAnimated(
	duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong(),
	fade: Boolean = true
) {
	animation?.cancel()
	animate()
		.setDuration((duration * alpha).toLong())
		.alpha(if (fade) 0f else 1f)
		.setInterpolator(AccelerateInterpolator())
		.setListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator?) {
				visibility = View.GONE
			}
		})
}

fun View.rotate(
	toDegrees: Float,
	duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong(),
	resetToDegrees: Float? = null
) {
	animation?.cancel()

	animate()
		.setDuration(duration)
		.rotation(toDegrees)
		.setInterpolator(AccelerateDecelerateInterpolator())
		.setListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator?) {
				resetToDegrees?.let { rotation = it }
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

fun View.setCutOutCardBackground() {
	val cutOutPositionPercentage = 0.6f

	val cutOutRadius = context.resources.getDimension(R.dimen.card_cutout_radius)

	// The left edge measures from the top to the bottom while the right edge measures from the bottom to the top
	val backgroundShape = ShapeAppearanceModel.builder()
		.setAllCornerSizes(context.resources.getDimension(R.dimen.corner_radius_sheet))
		.setLeftEdge(CutOutEdgeTreatment(cutOutRadius, 1 - cutOutPositionPercentage))
		.setRightEdge(CutOutEdgeTreatment(cutOutRadius, cutOutPositionPercentage))
		.build()

	val backgroundDrawable = MaterialShapeDrawable(backgroundShape).apply {
		fillColor = ContextCompat.getColorStateList(context, R.color.white)
		elevation = context.resources.getDimension(R.dimen.certificates_elevation)
		shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
	}

	background = backgroundDrawable
}