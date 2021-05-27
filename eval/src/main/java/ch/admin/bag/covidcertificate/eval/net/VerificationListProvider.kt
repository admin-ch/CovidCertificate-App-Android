package ch.admin.bag.covidcertificate.eval.net

import android.content.Context
import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.RevokedList
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class VerificationListProvider private constructor(spec: VerificationListProviderSpec) {

	companion object : SingletonHolder<VerificationListProvider, VerificationListProviderSpec>(::VerificationListProvider)

	private val verificationListService: VerificationListService

	init {
		val okHttpBuilder = OkHttpClient.Builder()

		val cacheSize = 5 * 1024 * 1024 // 5 MB
		val cache = Cache(spec.context.cacheDir, cacheSize.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		verificationListService = Retrofit.Builder()
			.baseUrl(spec.baseUrl)
			.client(okHttpBuilder.build())
			.addConverterFactory(MoshiConverterFactory.create())
			.build()
			.create(VerificationListService::class.java)
	}

	suspend fun getSigningKeysList(): Jwks? {
		return try {
			val response = withContext(Dispatchers.IO) { verificationListService.getSigningKeysList() }
			if (!response.isSuccessful) throw HttpException(response)
			response.body()
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	suspend fun getRevokedList(): RevokedList? {
		return try {
			val response = withContext(Dispatchers.IO) { verificationListService.getRevokedList() }
			if (!response.isSuccessful) throw HttpException(response)
			response.body()
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	suspend fun getNationalRules(): RuleSet? {
		return try {
			val response = withContext(Dispatchers.IO) { verificationListService.getRuleSet() }
			if (!response.isSuccessful) throw  HttpException(response)
			response.body()
		} catch (e: java.lang.Exception) {
			e.printStackTrace()
			null
		}
	}

}

data class VerificationListProviderSpec(val context: Context, val baseUrl: String)