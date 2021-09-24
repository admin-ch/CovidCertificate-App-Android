package ch.admin.bag.covidcertificate.common.qr

import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class QRCodeMixedXingAnalyzer(
	val coroutineScope: CoroutineScope,
	private val onDecodeCertificate: (decodeCertificateState: DecodeCertificateState) -> Unit
) : ImageAnalysis.Analyzer {
	companion object {
		private const val QR_CODE_ERROR_WRONG_FORMAT = "Q|YWF"
	}

	private val isGlobalHistogramBinarizer: AtomicBoolean = AtomicBoolean(true)

	private val yuvFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
	private val reader = MultiFormatReader().apply {
		val map = mapOf(
			DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
			DecodeHintType.TRY_HARDER to true,
		)
		setHints(map)
	}

	override fun analyze(imageProxy: ImageProxy) {
		if (isGlobalHistogramBinarizer.getAndSet(!isGlobalHistogramBinarizer.get())) {
			coroutineScope.launch { decodeWithGlobalHistogramBinarizer(imageProxy) }
		} else {
			decodeWithHybridBinarizer(imageProxy)
		}
	}


	suspend fun decodeWithGlobalHistogramBinarizer(imageProxy: ImageProxy) = withContext(Dispatchers.IO) {
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
				val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
				try {
					val result: Result = reader.decodeWithState(binaryBitmap)
					onDecodeCertificate(DecodeCertificateState.SUCCESS(result.text))
				} catch (e: NotFoundException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
					e.printStackTrace()
				} catch (e: ChecksumException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
					e.printStackTrace()
				} catch (e: FormatException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
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

	private fun decodeWithHybridBinarizer(imageProxy: ImageProxy) {
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
				val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
				try {
					val result: Result = reader.decodeWithState(binaryBitmap)
					onDecodeCertificate(DecodeCertificateState.SUCCESS(result.text))
				} catch (e: NotFoundException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
					e.printStackTrace()
				} catch (e: ChecksumException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
					e.printStackTrace()
				} catch (e: FormatException) {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
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
