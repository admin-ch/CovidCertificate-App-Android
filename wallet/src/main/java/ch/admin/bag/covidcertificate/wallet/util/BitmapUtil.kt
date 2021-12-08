package ch.admin.bag.covidcertificate.wallet.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.ActiveModes
import ch.admin.bag.covidcertificate.wallet.R

object BitmapUtil {
	fun getHumanReadableName(mode: String): String {
		val activeModes: List<ActiveModes> = CovidCertificateSdk.Wallet.getActiveModes().value
		return activeModes.find { activeMode -> activeMode.id == mode }?.displayName ?: mode
	}

	fun textAsBitmap(
		context: Context,
		text: String,
		textSize: Int,
		@ColorInt textColor: Int,
		@ColorInt backgroundColor: Int,
		isNotOK: Boolean = false
	): Bitmap? {
		val customTypeface = ResourcesCompat.getFont(context, R.font.inter_bold)
		val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		paint.textSize = textSize.toFloat()
		paint.color = textColor
		paint.textAlign = Paint.Align.LEFT
		paint.typeface = customTypeface
		val baseline: Float = -paint.ascent()
		val width = (paint.measureText(text) + 0.5f).toInt()
		val height = (baseline + paint.descent() + 0.5f).toInt()
		val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(image)
		canvas.drawText(text, 0.0f, baseline, paint)
		if (isNotOK) {
			paint.strokeWidth = 5.0f
			canvas.drawLine(10.0f, 3.0f, width.toFloat() - 12.0f, height.toFloat() - 3.0f, paint)
			paint.color = backgroundColor
			canvas.drawLine(14.0f, 0.0f, width.toFloat() - 8.0f, height.toFloat() - 6.0f, paint)
		}
		return image
	}
}