package ch.admin.bag.covidcertificate.common.net

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class UserAgentInterceptor(private val userAgentGenerator: UserAgentGenerator) : Interceptor {
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val request: Request = chain.request()
			.newBuilder()
			.header("User-Agent", userAgentGenerator.userAgent())
			.build()
		return chain.proceed(request)
	}

	fun interface UserAgentGenerator {
		fun userAgent(): String
	}
}