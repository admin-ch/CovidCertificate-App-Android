package ch.admin.bag.covidcertificate.common.qr

import android.media.SimpleImage
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream


@RunWith(Parameterized::class)
class QrMLCodeTests(val simpleImage: SimpleImage, val path: String) {

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
	fun checkCertWithMLKit() {

		var success: Boolean = false
		val image = InputImage.fromMediaImage(simpleImage, 0)
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