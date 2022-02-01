package ch.admin.bag.covidcertificate.wallet.networking.interceptor

import android.content.Context
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.extensions.updateLocale
import ch.admin.bag.covidcertificate.wallet.R
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class AcceptLanguageHeaderInterceptor(val applicationContext: Context) : Interceptor {

	private val configSecureStorage = ConfigSecureStorage.getInstance(applicationContext)
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		 val localeAdjustedContext = applicationContext.updateLocale(configSecureStorage.getUserLanguage())
		 val languageKey = localeAdjustedContext.getString(R.string.language_key)

		val request: Request = chain.request()
			.newBuilder()
			.header("Accept-Language", languageKey)
			.build()
		return chain.proceed(request)
	}
}