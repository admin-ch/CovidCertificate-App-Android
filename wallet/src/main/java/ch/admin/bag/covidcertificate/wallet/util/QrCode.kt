/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.util

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Parcel
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrCode private constructor(val data: String, val size: Int) {

	companion object {
		const val DEFAULT_BITMAP_SIZE = 1024
		private val DEFAULT_ECC = ErrorCorrectionLevel.M
		private val DEFAULT_CHARSET = "ISO-8859-1"

		fun create(value: String): QrCode {
			return QrCode(value, encode(value, 0).width)
		}

		private fun encode(value: String, size: Int) = MultiFormatWriter().encode(
			value,
			BarcodeFormat.QR_CODE,
			size,
			size,
			mapOf(EncodeHintType.ERROR_CORRECTION to DEFAULT_ECC, EncodeHintType.CHARACTER_SET to DEFAULT_CHARSET)
		)

		fun renderToBitmap(data: String, outSize: Int = 0): Bitmap {
			val bitMatrix = encode(data, outSize)
			val bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
			for (x in 0 until bitMatrix.width) {
				for (y in 0 until bitMatrix.height) {
					bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
				}
			}
			return bitmap
		}

		fun convertBitmapToBw(input: Bitmap): Bitmap {
			val mutableBitmap = input.copy(input.config, true)
			for(x in 0 until mutableBitmap.width){
				for(y in 0 until mutableBitmap.height) {
					val p = mutableBitmap[x, y]
					if(p != Color.WHITE && p != Color.BLACK) {
						mutableBitmap[x, y] = Color.BLACK
					}
				}
			}
			return mutableBitmap
		}
	}


	constructor(parcel: Parcel) : this(
		parcel.readString() ?: throw IllegalArgumentException("No data string in QrCode parcel!"),
		parcel.readInt()
	)

	fun renderToBitmap(outSize: Int = 0): Bitmap {
		return renderToBitmap(data, outSize)
	}
}