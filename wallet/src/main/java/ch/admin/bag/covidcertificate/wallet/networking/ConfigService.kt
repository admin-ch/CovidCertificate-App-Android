package ch.admin.bag.covidcertificate.wallet.networking

import ch.admin.bag.covidcertificate.common.config.ConfigModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ConfigService {

	@Headers("Accept: application/json")
	@GET("config")
	suspend fun getConfig(
		@Query("appversion") appVersion: String,
		@Query("osversion") osVersion: String,
		@Query("buildnr") buildNumber: String
	): Response<ConfigModel>

}