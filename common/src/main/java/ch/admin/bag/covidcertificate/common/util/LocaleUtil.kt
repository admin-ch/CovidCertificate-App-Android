package ch.admin.bag.covidcertificate.common.util

import android.content.Context
import ch.admin.bag.covidcertificate.common.R

object LocaleUtil {
	fun isSystemLangNotEnglish(context: Context): Boolean {
		return context.getString(R.string.language_key) != "en"
	}
}