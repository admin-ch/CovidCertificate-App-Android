package ch.admin.bag.covidcertificate.common.qr

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.admin.bag.covidcertificate.eval.data.state.Error
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QRCodeAnalyzer(
	private val onDecodeCertificate: (decodeCertificateState: DecodeCertificateState) -> Unit
) : ImageAnalysis.Analyzer {

	val QR_CODE_ERROR_WRONG_FORMAT = "Q|YWF"

	private val yuvFormats = mutableListOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)

	private val reader = MultiFormatReader().apply {
		val map = mapOf(
			DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
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
				onDecodeCertificate(DecodeCertificateState.ERROR(Error(QR_CODE_ERROR_WRONG_FORMAT)))
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
	data class SUCCESS(val qrCode: String?) : DecodeCertificateState()
	object SCANNING : DecodeCertificateState()
	data class ERROR(val error: Error) : DecodeCertificateState()
}

