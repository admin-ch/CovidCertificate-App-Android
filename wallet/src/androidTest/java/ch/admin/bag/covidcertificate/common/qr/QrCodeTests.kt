package ch.admin.bag.covidcertificate.common.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class QrCodeTests(val bitmap: Bitmap, val path: String) {

	companion object {
		@JvmStatic
		@Parameterized.Parameters
		fun data(): List<Array<Any>> {
			val context = InstrumentationRegistry.getInstrumentation().context
			val directory = context.assets.list("bitmaps")!!.asList()
			return directory.map { path ->
				val inputStream = context.assets.open("bitmaps/$path")
				arrayOf(BitmapFactory.decodeStream(inputStream), path)
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

		val intArray = IntArray(bitmap.width * bitmap.height)
		bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
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

}