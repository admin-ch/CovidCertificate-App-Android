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

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer


class QRCodeAnalyzer(
	private val onDecodeCertificate: (decodeCertificateState: DecodeCertificateState) -> Unit
) : ImageAnalysis.Analyzer {

	companion object {
		private const val QR_CODE_ERROR_WRONG_FORMAT = "Q|YWF"
	}

	private val yuvFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)

	private val reader = MultiFormatReader().apply {
		val map = mapOf(
			DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
			DecodeHintType.TRY_HARDER to true,
		)
		setHints(map)
	}

	override fun analyze(imageProxy: ImageProxy) {
		try {
			if (imageProxy.format in yuvFormats && imageProxy.planes.size == 3) {
				val data = imageProxy.planes[0].buffer.toByteArray()
				val source = PlanarYUVLuminanceSource(
					data,
					imageProxy.planes[0].rowStride,
					imageProxy.height,
					0,
					0,
					imageProxy.width,
					imageProxy.height,
					false
				)
				val pixels = source.renderThumbnail();
				val width = source.getThumbnailWidth();
				val height = source.getThumbnailHeight();
				val bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
				val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
				try {
					val result: Result = reader.decodeWithState(binaryBitmap)
					onDecodeCertificate(DecodeCertificateState.SUCCESS(result.text, bitmap))
				} catch (e: NotFoundException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING(bitmap))
					e.printStackTrace()
				} catch (e: ChecksumException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING(bitmap))
					e.printStackTrace()
				} catch (e: FormatException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING(bitmap))
					e.printStackTrace()
				}

			} else {
				onDecodeCertificate(DecodeCertificateState.ERROR(StateError(QR_CODE_ERROR_WRONG_FORMAT)))
			}
		} catch (e: IllegalStateException) {
			e.printStackTrace()
		} finally {
			imageProxy.close()
		}
	}

	private fun ByteBuffer.toByteArray(): ByteArray {
		rewind()
		val data = ByteArray(remaining())
		get(data)
		return data
	}
}

sealed class DecodeCertificateState {
	data class SUCCESS(val qrCode: String?, val bitmap: Bitmap) : DecodeCertificateState()
	data class SCANNING(val bitmap: Bitmap) : DecodeCertificateState()
	data class ERROR(val error: StateError) : DecodeCertificateState()
}
