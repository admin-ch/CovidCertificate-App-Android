package ch.admin.bag.covidcertificate.common.extensions

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun OkHttpClient.Builder.setTimeouts(seconds: Long = 10): OkHttpClient.Builder {
	connectTimeout(seconds, TimeUnit.SECONDS)
	writeTimeout(seconds, TimeUnit.SECONDS)
	readTimeout(seconds, TimeUnit.SECONDS)
	return this
}