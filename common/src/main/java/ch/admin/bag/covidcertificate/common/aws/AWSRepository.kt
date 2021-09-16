package ch.admin.bag.covidcertificate.common.aws

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream

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

	suspend fun upload(bitmap: Bitmap) {
		val awsRequestBody = AWSBody(convert(bitmap))
		awsService.upload(awsRequestBody)
	}


	fun convert(bitmap: Bitmap): String {
		val outputStream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
		return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
	}
}

class AWSSpec(val context: Context, val apiKey: String)