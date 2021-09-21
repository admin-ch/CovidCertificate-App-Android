package ch.admin.bag.covidcertificate.common.qr

import android.media.SimpleImage
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream


@RunWith(Parameterized::class)
class QrCodeTests(val simpleImage: SimpleImage, val path: String) {

	companion object {
		@JvmStatic
		@Parameterized.Parameters
		fun data(): List<Array<Any>> {
			val context = InstrumentationRegistry.getInstrumentation().context
			val directory = context.assets.list("bitmaps")!!.asList()
			return directory.map { path ->
				val inputStream = GZIPInputStream(context.assets.open("bitmaps/$path"))
				arrayOf(Gson().fromJson(InputStreamReader(inputStream), SimpleImage::class.java), path)
			}
		}
	}

	@Test
	fun checkCert() {
		val hints = mapOf(
			DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
			DecodeHintType.TRY_HARDER to true,
		)
		val reader = MultiFormatReader().apply { setHints(hints) }
		var success = false

		val yBuffer = simpleImage.planes[0].buffer
		val data = yBuffer.toByteArray()
		val source = PlanarYUVLuminanceSource(
			data,
			simpleImage.planes[0].rowStride,
			simpleImage.height,
			0,
			0,
			simpleImage.width,
			simpleImage.height,
			false
		)
		val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
		try {
			val result = reader.decodeWithState(binaryBitmap)
			success = true
		} catch (e: NotFoundException) {
			e.printStackTrace()
		} catch (e: ChecksumException) {
			e.printStackTrace()
		} catch (e: FormatException) {
			e.printStackTrace()
		}
		assert(success) { path }
	}

	private fun ByteBuffer.toByteArray(): ByteArray {
		rewind()
		val data = ByteArray(remaining())
		get(data)
		return data
	}
}