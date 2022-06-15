/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.renewal.net

import android.content.Context
import ch.admin.bag.covidcertificate.common.extensions.setTimeouts
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.CertificatePinning
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.JwsInterceptor
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.renewal.model.QrCodeRenewalResponse
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class QrCodeRenewalRepository private constructor(context: Context) {

	companion object : SingletonHolder<QrCodeRenewalRepository, Context>(::QrCodeRenewalRepository) {
		private const val STATUS_CODE_TOO_MANY_REQUESTS = 429
	}

	private val renewalService: QrCodeRenewalService

	init {
		val rootCa = CovidCertificateSdk.getRootCa(context)
		val expectedCommonName = CovidCertificateSdk.getExpectedCommonName()
		val okHttpBuilder = OkHttpClient.Builder()
			.certificatePinner(CertificatePinning.pinner)
			.addInterceptor(JwsInterceptor(rootCa, expectedCommonName))
			.addInterceptor(UserAgentInterceptor(Config.userAgent))
			.setTimeouts()

		val cacheSize = 5 * 1024 * 1024 // 5 MB
		val cache = Cache(context.cacheDir, cacheSize.toLong())
		okHttpBuilder.cache(cache)

		if (BuildConfig.DEBUG) {
			val httpInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
			okHttpBuilder.addInterceptor(httpInterceptor)
		}

		renewalService = Retrofit.Builder()
			.baseUrl(BuildConfig.BASE_URL_TRANSFORMATION)
			.client(okHttpBuilder.build())
			.addConverterFactory(MoshiConverterFactory.create())
			.build()
			.create(QrCodeRenewalService::class.java)
	}

	suspend fun renew(certificateHolder: CertificateHolder): QrCodeRenewalResponse {
		if (certificateHolder.containsChLightCert()) return QrCodeRenewalResponse.Failed

		val body = QrCodeRenewalBody(certificateHolder.qrCodeData)
		val response = renewalService.renew(body)

		return when {
			response.isSuccessful -> {
				response.body()?.let { QrCodeRenewalResponse.Success(it.hcert) } ?: QrCodeRenewalResponse.Failed
			}
			response.code() == STATUS_CODE_TOO_MANY_REQUESTS -> QrCodeRenewalResponse.RateLimitExceeded
			else -> QrCodeRenewalResponse.Failed
		}
	}
}