package ch.admin.bag.covidcertificate.common.extensions

import android.view.Window
import android.view.WindowManager

fun Window.overrideScreenBrightness(override: Boolean) {
	if (override) {
		addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	} else {
		clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}
	val layoutParams: WindowManager.LayoutParams = attributes
	layoutParams.screenBrightness = if (override) 1f else -1f
	attributes = layoutParams
}