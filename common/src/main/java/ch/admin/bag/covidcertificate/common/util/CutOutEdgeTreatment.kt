/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.util

import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.ShapePath
import kotlin.math.atan
import kotlin.math.sqrt

class CutOutEdgeTreatment(
	private val radius: Float,
	private val positionPercentage: Float
) : EdgeTreatment() {

	override fun getEdgePath(length: Float, center: Float, interpolation: Float, shapePath: ShapePath) {
		val cutOutCenter = length * positionPercentage

		// Calculate the vertical offset of the cut out. When interpolating the edge, the offset is the perpendicular distance
		// from the cut out center to the edge. When fully interpolated (1.0), the offset will be zero, while at zero interpolation
		// the offset will be equal to the radius
		val verticalOffset = (1.0f - interpolation) * radius
		val verticalOffsetRatio = verticalOffset / radius
		if (verticalOffsetRatio >= 1.0f) {
			// The vertical offset is so high that there would be no cut out drawn in the edge, so just draw a straight line along the edge
			shapePath.lineTo(length, 0.0f)
			return
		}

		// Calculate the X distance between the center and edge of the cut out, taking the interpolation value into consideration
		// When fully interpolated (1.0) the radiusX is the same as the radius
		val squaredRadius = radius * radius
		val verticalOffsetSquared = verticalOffset * verticalOffset
		val radiusX = sqrt((squaredRadius - verticalOffsetSquared).toDouble()).toFloat()

		// Draw a line from the start of the edge to the start of the cut out
		val cutOutStartX = cutOutCenter - radiusX
		shapePath.lineTo(cutOutStartX, 0.0f)

		// Calculate the arc of the cut out
		val cornerRadiusArcLength = Math.toDegrees(atan((radiusX / verticalOffset).toDouble())).toFloat()
		val cutoutArcOffset = 90.0f - cornerRadiusArcLength

		// Draw the cut out arc
		shapePath.addArc(
			cutOutCenter - radius,
			-radius - verticalOffset,
			cutOutCenter + radius,
			radius - verticalOffset,
			180.0f - cutoutArcOffset,
			cutoutArcOffset * 2.0f - 180.0f
		)

		// Draw a line from the end of the cut out to the end of the edge
		shapePath.lineTo(length, 0.0f)
	}


}