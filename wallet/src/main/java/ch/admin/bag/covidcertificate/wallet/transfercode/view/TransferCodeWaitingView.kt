/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode.view

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import ch.admin.bag.covidcertificate.wallet.databinding.ViewTransferCodeWaitingBinding

class TransferCodeWaitingView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	private val binding = ViewTransferCodeWaitingBinding.inflate(LayoutInflater.from(context), this)

	init {
		clipToPadding = false
		clipChildren = false
	}

	override fun onFinishInflate() {
		super.onFinishInflate()

		// Animate ripple
		val rippleScaleX = PropertyValuesHolder.ofFloat(SCALE_X, 1f, 3f)
		val rippleScaleY = PropertyValuesHolder.ofFloat(SCALE_Y, 1f, 3f)
		val rippleAlpha = PropertyValuesHolder.ofFloat(ALPHA, 1f, 0f)
		val rippleAnimation = ObjectAnimator.ofPropertyValuesHolder(binding.illuTransferCodeWaitingRipple, rippleScaleX, rippleScaleY, rippleAlpha).apply {
			duration = 1000L
			startDelay = 8000L
			interpolator = AccelerateInterpolator()
			addListener(
				// startDelay is ignored in repeat, so do a manual repeat
				onEnd = { start() }
			)
		}
		rippleAnimation.start()

		// Animate phone
		ObjectAnimator.ofFloat(binding.illuTransferCodeWaitingPhone, TRANSLATION_Y, 0f, -15f).apply {
			repeatCount = ValueAnimator.INFINITE
			repeatMode = ValueAnimator.REVERSE
			interpolator = AccelerateDecelerateInterpolator()
			duration = 1500L
		}.start()

		// Animate shadow
		val shadowScaleX = PropertyValuesHolder.ofFloat(SCALE_X, 1f, 0.85f)
		val shadowScaleY = PropertyValuesHolder.ofFloat(SCALE_Y, 1f, 0.85f)
		ObjectAnimator.ofPropertyValuesHolder(binding.illuTransferCodeWaitingShadow, shadowScaleX, shadowScaleY).apply {
			repeatCount = ValueAnimator.INFINITE
			repeatMode = ValueAnimator.REVERSE
			interpolator = AccelerateDecelerateInterpolator()
			duration = 1500L
		}.start()
	}

}