package ch.admin.bag.covidcertificate.common.qr

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QRCodeMLKitAnalyzer(
	val coroutineScope: CoroutineScope,
	private val onDecodeCertificate: (decodeCertificateState: DecodeCertificateState) -> Unit
) :
	ImageAnalysis.Analyzer {
	val options = BarcodeScannerOptions.Builder()
		.setBarcodeFormats(
			Barcode.FORMAT_QR_CODE
		)
		.build()


	private val scanner = BarcodeScanning.getClient(options)

	override fun analyze(imageProxy: ImageProxy) {
		coroutineScope.launch { decode(imageProxy) }
	}

	@SuppressLint("UnsafeOptInUsageError")
	suspend fun decode(imageProxy: ImageProxy) = withContext(Dispatchers.IO) {
		try {
			if (imageProxy.image != null) {
				val inputImage: InputImage = InputImage.fromMediaImage(imageProxy.image, imageProxy.imageInfo.rotationDegrees)
				val task = scanner.process(inputImage)
				val barcodes = Tasks.await(task)
				if (barcodes.size > 0) {
					val result = barcodes[0].displayValue
					onDecodeCertificate(DecodeCertificateState.SUCCESS((result)))
				} else {
					onDecodeCertificate(DecodeCertificateState.SCANNING)
				}
			} else {
				onDecodeCertificate(DecodeCertificateState.SCANNING)
			}
		} catch (e: Exception) {
			onDecodeCertificate(DecodeCertificateState.SCANNING)
			e.printStackTrace()
		} finally {
			imageProxy.close()
		}
	}
}