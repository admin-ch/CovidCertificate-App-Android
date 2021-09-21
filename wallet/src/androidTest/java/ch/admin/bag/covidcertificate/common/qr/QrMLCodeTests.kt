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
class QrMLCodeTests(val bitmap: Bitmap, val path: String) {

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
	fun checkCertWithMLKit() {

		var success: Boolean = false
		val image = InputImage.fromBitmap(bitmap, 0)
		val options = BarcodeScannerOptions.Builder()
			.setBarcodeFormats(
				Barcode.FORMAT_QR_CODE
			)
			.build()
		val scanner = BarcodeScanning.getClient(options)

		val task = scanner.process(image)

		try {
			val barcodes = Tasks.await(task)
			if (barcodes.size > 0 && barcodes[0].displayValue.contains("HC1:")) {
				Log.d("path ", path)
				success = true
			} else {
				success = false
			}

		} catch (e: Exception) {
			success = false
		}

		assert(success) { path }
	}
}