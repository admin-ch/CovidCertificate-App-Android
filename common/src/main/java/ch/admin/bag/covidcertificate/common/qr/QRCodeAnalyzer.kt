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

import android.annotation.SuppressLint
import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream
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

	@SuppressLint("UnsafeOptInUsageError")
	override fun analyze(imageProxy: ImageProxy) = try {
		if (imageProxy.format in yuvFormats && imageProxy.image != null && imageProxy.image!!.planes.size == 3) {
			val yBuffer = imageProxy.planes[0].buffer
			val bitmap = imageProxy.let { toBitmap(it, yBuffer) }
			val data = yBuffer.toByteArray()
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

	private fun toBitmap(image: ImageProxy, yBuffer: ByteBuffer): Bitmap? {
		val planes: Array<ImageProxy.PlaneProxy> = image.planes
		val uBuffer: ByteBuffer = planes[1].buffer
		val vBuffer: ByteBuffer = planes[2].buffer
		val ySize = yBuffer.remaining()
		val uSize = uBuffer.remaining()
		val vSize = vBuffer.remaining()
		val nv21 = ByteArray(ySize + uSize + vSize)
		yBuffer[nv21, 0, ySize]
		vBuffer[nv21, ySize, vSize]
		uBuffer[nv21, ySize + vSize, uSize]
		val intArray = IntArray(3)
		intArray[0] = image.planes[0].rowStride
		intArray[1] = image.planes[1].rowStride
		intArray[2] = image.planes[2].rowStride
		val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, intArray)
		val out = ByteArrayOutputStream()
		yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
		val imageBytes = out.toByteArray()
		return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
	}

	private fun ByteBuffer.toByteArray(): ByteArray {
		rewind()
		val data = ByteArray(remaining())
		get(data)
		return data
	}
}

sealed class DecodeCertificateState {
	data class SUCCESS(val qrCode: String?, val bitmap: Bitmap?) : DecodeCertificateState()
	data class SCANNING(val bitmap: Bitmap?) : DecodeCertificateState()
	data class ERROR(val error: StateError) : DecodeCertificateState()
}
