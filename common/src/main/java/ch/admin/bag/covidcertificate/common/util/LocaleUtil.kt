package ch.admin.bag.covidcertificate.common.util

import android.content.Context
import android.content.res.Configuration
import ch.admin.bag.covidcertificate.common.R
import java.util.*

object LocaleUtil {

	const val DEFAULT_COUNTRY = "CH"

	fun isSystemLangNotEnglish(context: Context): Boolean {
		return context.getString(R.string.language_key) != "en"
	}

	fun updateLanguage(context: Context, language: String?): Context {
		if (!language.isNullOrEmpty()) {
			val config = Configuration()
			config.setLocale(Locale(language, DEFAULT_COUNTRY))
			return context.createConfigurationContext(config)
		}
		return context
	}

}