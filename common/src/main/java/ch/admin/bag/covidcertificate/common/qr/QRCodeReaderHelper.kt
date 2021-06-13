package ch.admin.bag.covidcertificate.common.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.lang.Boolean
import java.util.*


object QRCodeReaderHelper {

	fun decodeQrCode(bitmap: Bitmap): String? {
		var decoded: String? = null
		val intArray = IntArray(bitmap.width * bitmap.height)
		bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width,
			bitmap.height)
		val source: LuminanceSource = RGBLuminanceSource(bitmap.width,
			bitmap.height, intArray)
		val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
		val reader = MultiFormatReader()
		try {
			val result: Result = reader.decodeWithState(binaryBitmap)
			decoded = result.text
		} catch (e: NotFoundException) {
			e.printStackTrace()
		} catch (e: ChecksumException) {
			e.printStackTrace()
		} catch (e: FormatException) {
			e.printStackTrace()
		}
		return decoded
	}

	fun pdfToBitmap(context: Context, pdfFile: File): ArrayList<Bitmap> {
		val bitmaps = arrayListOf<Bitmap>()
		try {
			val renderer = PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));
			var bitmap: Bitmap?
			val pageCount = renderer.pageCount;
			for (i in 0 until pageCount) {
				val page: PdfRenderer.Page = renderer.openPage(i);
				var width = context.resources.displayMetrics.densityDpi / 72 * page.width;
				var height = context.resources.displayMetrics.densityDpi / 72 * page.height;
				bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

				val canvas = Canvas(bitmap)
				canvas.drawColor(Color.WHITE)
				canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)

				val r = Rect(0, 0, width, height);
				page.render(bitmap, r, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
				bitmaps.add(bitmap);
				// close the page
				page.close();
			}
			// close the renderer
			renderer.close();

		} catch (ex: Exception) {
			ex.printStackTrace();
		}

		return bitmaps
	}
}

