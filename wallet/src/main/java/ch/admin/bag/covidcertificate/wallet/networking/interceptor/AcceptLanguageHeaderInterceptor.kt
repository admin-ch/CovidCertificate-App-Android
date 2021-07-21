package ch.admin.bag.covidcertificate.wallet.networking.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class AcceptLanguageHeaderInterceptor(val lang: String) : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request: Request = chain.request()
			.newBuilder()
			.header("Accept-Language", lang)
			.build()
		return chain.proceed(request)
	}
}