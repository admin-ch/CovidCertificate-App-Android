/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.light

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.light.model.CertificateLightConversionState
import ch.admin.bag.covidcertificate.wallet.light.net.CertificateLightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class CertificateLightViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)
	private val repository = CertificateLightRepository.getInstance(application.applicationContext)

	private val conversionStateMutableLiveData = MutableLiveData<CertificateLightConversionState>()
	val conversionState = conversionStateMutableLiveData as LiveData<CertificateLightConversionState>

	private var conversionJob: Job? = null

	fun convert(certificateHolder: CertificateHolder) {
		conversionJob?.cancel()

		conversionStateMutableLiveData.value = CertificateLightConversionState.LOADING
		conversionJob = viewModelScope.launch(Dispatchers.IO) {
			try {
				val response = repository.convert(certificateHolder)

				if (response != null) {
					val decodeState = CovidCertificateSdk.Wallet.decode(response.payload)
					when (decodeState) {
						is DecodeState.SUCCESS -> {
							walletDataStorage.storeCertificateLight(
								certificateHolder,
								decodeState.certificateHolder.qrCodeData,
								response.qrcode
							)

							conversionStateMutableLiveData.postValue(
								CertificateLightConversionState.SUCCESS(
									decodeState.certificateHolder,
									response.qrcode
								)
							)
						}
						is DecodeState.ERROR -> {
							conversionStateMutableLiveData.postValue(CertificateLightConversionState.ERROR(decodeState.error))
						}
					}
				} else {
					checkIfOffline()
				}
			} catch (e: IOException) {
				checkIfOffline()
			}
		}
	}

	/**
	 * Delete the certificate light in the storage and return the original certificate
	 * @return The original certificate to which this certificate light belonged, or null if it didn't exist in the storage or
	 * 			could not be decoded (both should never be the case)
	 */
	fun deleteCertificateLight(certificateHolder: CertificateHolder): CertificateHolder? {
		val regularCertificate = walletDataStorage.deleteCertificateLight(certificateHolder.qrCodeData)
		return regularCertificate?.let {
			val decodeState = CovidCertificateSdk.Wallet.decode(it.qrCodeData)
			if (decodeState is DecodeState.SUCCESS) {
				decodeState.certificateHolder
			} else {
				null
			}
		}
	}

	private fun checkIfOffline() {
		val error = if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
			StateError(ErrorCodes.GENERAL_NETWORK_FAILURE)
		} else {
			StateError(ErrorCodes.GENERAL_OFFLINE)
		}
		conversionStateMutableLiveData.postValue(CertificateLightConversionState.ERROR(error))
	}

}