/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.common.qr

import com.google.zxing.LuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import okhttp3.internal.and
import kotlin.math.abs

/**
 * Use a Kittler Binarizer described here //http://www.iztok-jr-fister.eu/static/publications/39.pdf
 * Currently not in use, but may be in the future for more scanner improvements
 */
class KittlerBinarizer(luminanceSource: LuminanceSource) : GlobalHistogramBinarizer(luminanceSource) {

	override fun getBlackMatrix(): BitMatrix {
		val source = luminanceSource
		val width = source.width
		val height = source.height
		var count = 0
		var totalcount = 0
		val matrix = BitMatrix(width, height)
		val localLuminanceSource = source.matrix
		val threshold = estimateThreshold(localLuminanceSource, width, height)
		(0 until height).forEach { y ->
			val offset = y * width
			(0 until width).forEach { x ->
				totalcount++
				val pixel = localLuminanceSource[offset + x] and 0xFF

				if (pixel < threshold) {
					count++
					matrix.set(x, y)
				}
			}

		}
		return matrix
	}

	private fun estimateThreshold(localLuminanceSource: ByteArray, width: Int, height: Int): Int {
		var E = 0L
		var EF = 0L
		(1 until height - 1).forEach { y ->
			val offset = y * width
			(1 until width - 1).forEach { x ->
				val grey = localLuminanceSource[offset + x] and 0xFF
				val grey1 = localLuminanceSource[offset + x - 1] and 0xFF
				val grey2 = localLuminanceSource[offset + x + 1] and 0xFF
				val grey3 = localLuminanceSource[offset + x - width] and 0xFF
				val grey4 = localLuminanceSource[offset + x + width] and 0xFF

				val Ex = abs(grey1 - grey2)
				val Ey = abs(grey3 - grey4)
				val exy = Math.max(Ex, Ey)
				E += exy
				EF += exy * grey
			}
		}
		if (E == 0L) {
			return 128
		}
		return ((EF / E) - 1L).toInt()
	}
}