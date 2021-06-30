/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.net

import android.content.Context
import android.os.Build
import ch.admin.bag.covidcertificate.common.BuildConfig
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.util.AssetUtil
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.CertificatePinning
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.JwsInterceptor
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ConfigRepository private constructor(private val configSpec: ConfigSpec) {

	companion object : SingletonHolder<ConfigRepository, ConfigSpec>(::ConfigRepository) {
		private const val APP_VERSION_PREFIX_ANDROID = "android-"
		private const val OS_VERSION_PREFIX_ANDROID = "android"

		private const val MIN_LOAD_WAIT_TIME = 60 * 60 * 1000L // 1h
		private const val MAX_AGE_STATUS_VALID_CACHED_CONFIG = 48 * 60 * 60 * 1000L // 48h

		fun getCurrentConfig(context: Context) =
			ConfigSecureStorage.getInstance(context).getConfig() ?: AssetUtil.loadDefaultConfig(context)
	}

	private val configService: ConfigService
	private val storage = ConfigSecureStorage.getInstance(configSpec.context)

	init {
		val rootCa = CovidCertificateSdk.getRootCa(configSpec.context)
		val expectedCommonName = CovidCertificateSdk.getExpectedCommonName()
		val okHttpBuilder = OkHttpClient.Builder()
			.certificatePinner(CertificatePinning.pinner)
			.addInterceptor(JwsInterceptor(rootCa, expectedCommonName))
			.addInterceptor(UserAgentInterceptor(Config.userAgent))

		val cacheSize = 5 * 1024 * 1024 // 5 MB
		val cache = Cache(configSpec.context.cacheDir, cacheSize.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		configService = Retrofit.Builder()
			.baseUrl(configSpec.baseUrl)
			.client(okHttpBuilder.build())
			.addConverterFactory(MoshiConverterFactory.create())
			.build()
			.create(ConfigService::class.java)
	}

	suspend fun loadConfig(context: Context): ConfigModel? {
		val requestTimeStamp = System.currentTimeMillis()
		val appVersion = APP_VERSION_PREFIX_ANDROID + configSpec.versionName
		val osVersion = OS_VERSION_PREFIX_ANDROID + Build.VERSION.SDK_INT
		val buildNumber = configSpec.buildTime
		val versionString = "appversion=$appVersion&osversion=$osVersion&buildnr=$buildNumber"
		var config =
			if (storage.getConfigLastSuccessTimestamp() + MIN_LOAD_WAIT_TIME <= System.currentTimeMillis() || versionString != storage.getConfigLastSuccessAppAndOSVersion()) {
				try {
					val response = withContext(Dispatchers.IO) { configService.getConfig(appVersion, osVersion, buildNumber) }
					if (!response.isSuccessful) throw HttpException(response)
					response.body()?.let { storage.updateConfigData(it, requestTimeStamp, versionString) }
					response.body()
				} catch (e: Exception) {
					e.printStackTrace()
					null
				}
			} else null

		if (config == null) config = storage.getConfig()
		if (config == null) config = AssetUtil.loadDefaultConfig(context)

		return config
	}

}

class ConfigSpec(val context: Context, val baseUrl: String, val versionName: String, val buildTime: String)