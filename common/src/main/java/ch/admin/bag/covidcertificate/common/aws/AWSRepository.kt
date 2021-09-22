package ch.admin.bag.covidcertificate.common.aws

import android.content.Context
import android.util.Base64
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream


class AWSRepository private constructor(private val awsSpec: AWSSpec) {

	companion object : SingletonHolder<AWSRepository, AWSSpec>(::AWSRepository)

	private val awsService: AWSService

	init {
		val okHttpBuilder = OkHttpClient.Builder()
			.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
			.addInterceptor(
				Interceptor { chain ->
					val request: Request = chain.request()
						.newBuilder()
						.header("x-api-key", awsSpec.apiKey)
						.build()
					chain.proceed(request)
				})


		awsService = Retrofit.Builder()
			.baseUrl("https://3fvxlsht9f.execute-api.eu-central-1.amazonaws.com/default/")
			.client(okHttpBuilder.build())
			.addConverterFactory(MoshiConverterFactory.create())
			.build()
			.create(AWSService::class.java)
	}

	suspend fun upload(simpleImageAsJson: String, success: Boolean) {
		val awsRequestBody = AWSBody(convert(simpleImageAsJson), success)
		awsService.upload(awsRequestBody)
	}


	fun convert(simpleImageAsJson: String): String {
		val obj = ByteArrayOutputStream()
		val gzip = GZIPOutputStream(obj)
		gzip.write(simpleImageAsJson.toByteArray())
		gzip.flush()
		gzip.close()
		return Base64.encodeToString(obj.toByteArray(), Base64.DEFAULT)
	}
}

class AWSSpec(val context: Context, val apiKey: String)