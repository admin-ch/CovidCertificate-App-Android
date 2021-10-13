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

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.admin.bag.covidcertificate.common.qr.QRCodeMixedZXingAnalyzer.BinarizerFactory
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.*
import java.nio.ByteBuffer

/**
 * Mixed approach for QR Code Analyzer that uses multiple binarizers to analyze every camera frame:
 * - GlobalHistogramBinarizer
 * - GlobalHistogramBinarizer with inverted luminance source
 * - HybridBinarizer
 * - HybridBinarizer with inverted luminance source
 **/
class QRCodeMixedZXingAnalyzer(
	private val coroutineScope: CoroutineScope,
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

	private val binarizerFactories = listOf(
		BinarizerFactory { luminanceSource -> GlobalHistogramBinarizer(luminanceSource) },
		BinarizerFactory { luminanceSource -> GlobalHistogramBinarizer(luminanceSource.invert()) },
		BinarizerFactory { luminanceSource -> HybridBinarizer(luminanceSource) },
		BinarizerFactory { luminanceSource -> HybridBinarizer(luminanceSource.invert()) }
	)

	override fun analyze(imageProxy: ImageProxy) {
		coroutineScope.launch {
			// Convert the image proxy to a luminance source to be used by the binarizers
			val luminanceSource = createLuminanceSourceFromImageProxy(imageProxy)

			if (luminanceSource == null) {
				// ImageProxy contained the wrong format, display an error
				onDecodeCertificate.invoke(DecodeCertificateState.ERROR(StateError(QR_CODE_ERROR_WRONG_FORMAT)))
				imageProxy.close()
				return@launch
			}

			// Launch a deferred coroutine for each binarizer, decoding in parallel but waiting for all of them to finish
			val deferredDecodingStates = binarizerFactories.map {
				async {
					decodeFrame(luminanceSource, it)
				}
			}.awaitAll()

			if (isActive) {
				// Invoke the callback with either the first success state, the first error state or the SCANNING state
				val combinedDecodeState = deferredDecodingStates.firstOrNull { it is DecodeCertificateState.SUCCESS }
					?: deferredDecodingStates.firstOrNull { it is DecodeCertificateState.ERROR }
					?: DecodeCertificateState.SCANNING

				onDecodeCertificate.invoke(combinedDecodeState)
			}

			// Close the image proxy after the analysis, so that the camera frame capture isn't blocked
			imageProxy.close()
		}
	}

	private fun createLuminanceSourceFromImageProxy(imageProxy: ImageProxy): LuminanceSource? {
		return if (imageProxy.format in yuvFormats && imageProxy.planes.size == 3) {
			val data = imageProxy.planes[0].buffer.toByteArray()
			PlanarYUVLuminanceSource(
				data,
				imageProxy.planes[0].rowStride,
				imageProxy.height,
				0,
				0,
				imageProxy.width,
				imageProxy.height,
				false
			)
		} else {
			null
		}
	}

	private suspend fun decodeFrame(
		luminanceSource: LuminanceSource,
		binarizerFactory: BinarizerFactory
	): DecodeCertificateState = withContext(Dispatchers.Default) {
		try {
			val binarizer = binarizerFactory.createBinarizer(luminanceSource)
			val binaryBitmap = BinaryBitmap(binarizer)

			try {
				// Found QR code, return the decoded result
				val result: Result = reader.decodeWithState(binaryBitmap)
				return@withContext DecodeCertificateState.SUCCESS(result.text)
			} catch (e: NotFoundException) {
				// No QR code found. Keep scanning
				e.printStackTrace()
				return@withContext DecodeCertificateState.SCANNING
			} catch (e: ChecksumException) {
				// QR code found and decoded, but its checksum was invalid. Keep scanning
				e.printStackTrace()
				return@withContext DecodeCertificateState.SCANNING
			} catch (e: FormatException) {
				// QR code with invalid format detected, might be misdetected. Keep scanning
				e.printStackTrace()
				return@withContext DecodeCertificateState.SCANNING
			}
		} catch (e: IllegalStateException) {
			// Decoding failed. Keep scanning
			e.printStackTrace()
			return@withContext DecodeCertificateState.SCANNING
		}
	}

	private fun ByteBuffer.toByteArray(): ByteArray {
		rewind()
		val data = ByteArray(remaining())
		get(data)
		return data
	}

	private fun interface BinarizerFactory {
		fun createBinarizer(luminanceSource: LuminanceSource): Binarizer
	}
}
