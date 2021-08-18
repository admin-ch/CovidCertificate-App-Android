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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.lang.Integer.min
import kotlin.math.roundToInt


object QRCodeReaderHelper {
	private val hints = mapOf(
		DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
		DecodeHintType.TRY_HARDER to true,
	)

	private const val PDF_PAGE_LIMIT = 5
	private val reader = MultiFormatReader().apply { setHints(hints) }

	fun decodeQrCode(bitmap: Bitmap): String? {
		var decoded: String? = null

		val intArray = IntArray(bitmap.width * bitmap.height)
		bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
		val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

		try {
			val result: Result = reader.decodeWithState(binaryBitmap)
			decoded = result.text
		} catch (e: NotFoundException) {
			e.printStackTrace()
		} catch (e: ChecksumException) {
			e.printStackTrace()
		} catch (e: FormatException) {
			e.printStackTrace()
		}
		return decoded
	}

	fun pdfToBitmaps(context: Context, pdfFile: File): Sequence<Bitmap> = sequence {
		try {
			PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
				val pageCount = renderer.pageCount

				for (i in 0 until min(pageCount, PDF_PAGE_LIMIT)) {
					renderer.openPage(i).use { page ->
						// PDF width/height are given in "points pt" such that 1 pt = 1/72 inch
						// => 72 "dots-per-inch dpi" <==> scale = 1
						val scale: Float = context.resources.displayMetrics.densityDpi / 72f
						val pixelWidth = (scale * page.width).roundToInt()
						val pixelHeight = (scale * page.height).roundToInt()

						// Try both the scaled and the unscaled version
						page.renderToBitmap(page.width, page.height).use {
							yield(it)
						}
						page.renderToBitmap(pixelWidth, pixelHeight).use {
							yield(it)
						}
					}
				}
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
		}
	}

	private fun PdfRenderer.Page.renderToBitmap(pixelWidth: Int, pixelHeight: Int): Bitmap {
		val bitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888)

		// Make sure the bitmap's background is not transparent (which can cause issues for QR code detection)
		bitmap.eraseColor(Color.WHITE)

		// Draw the page onto the bitmap. Internally, this will scale the page to fit the bitmap (unless transform != null).
		this.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

		// reduce pixel size to avoid memory issues down the line
		val bitmapR = bitmap.copy(Bitmap.Config.RGB_565, false)
		bitmap.recycle()

		return bitmapR
	}

	private inline fun Bitmap.use(block: (Bitmap) -> Unit) {
		try {
			block(this)
		} finally {
			this.recycle()
		}
	}
}

