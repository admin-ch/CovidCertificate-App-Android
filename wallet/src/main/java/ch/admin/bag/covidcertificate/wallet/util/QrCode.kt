package ch.admin.bag.covidcertificate.wallet.util

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Parcel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrCode private constructor(val data: String, val size: Int) {

	companion object {
		const val DEFAULT_BITMAP_SIZE = 1024
		private val DEFAULT_ECC = ErrorCorrectionLevel.H

		fun create(value: String): QrCode {
			return QrCode(value, encode(value, 0).width)
		}

		private fun encode(value: String, size: Int) = MultiFormatWriter().encode(
			value,
			BarcodeFormat.QR_CODE,
			size,
			size,
			mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to DEFAULT_ECC)
		)

		fun renderToBitmap(data: String, outSize: Int = 0): Bitmap {
			val bitMatrix = encode(data, outSize)
			val bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
			for (x in 0 until bitMatrix.width) {
				for (y in 0 until bitMatrix.height) {
					bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
				}
			}
			return bitmap
		}
	}


	constructor(parcel: Parcel) : this(
		parcel.readString() ?: throw IllegalArgumentException("No data string in QrCode parcel!"),
		parcel.readInt()
	)

	fun renderToBitmap(outSize: Int = 0): Bitmap {
		return renderToBitmap(data, outSize)
	}
}