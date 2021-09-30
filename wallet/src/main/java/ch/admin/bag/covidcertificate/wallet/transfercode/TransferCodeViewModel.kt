/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.exception.TimeDeviationException
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.TransferCodeCrypto
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliveryRepository
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliverySpec
import kotlinx.coroutines.*
import java.io.IOException
import java.security.KeyPair

class TransferCodeViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)
	private val deliveryRepository =
		DeliveryRepository.getInstance(DeliverySpec(application.applicationContext, BuildConfig.BASE_URL_DELIVERY))

	private val conversionStateMutableLiveData = MutableLiveData<TransferCodeConversionState>(TransferCodeConversionState.LOADING)
	val conversionState = conversionStateMutableLiveData as LiveData<TransferCodeConversionState>

	private var downloadJob: Job? = null

	fun downloadCertificateForTransferCode(transferCode: TransferCodeModel, delayInMillis: Long = 0L) {
		downloadJob?.cancel()

		conversionStateMutableLiveData.value = TransferCodeConversionState.LOADING
		downloadJob = viewModelScope.launch(Dispatchers.IO) {
			val keyPair = TransferCodeCrypto.loadKeyPair(transferCode.code, getApplication())

			if (keyPair != null) {
				try {
					if (delayInMillis > 0) delay(delayInMillis)

					val decryptedCertificates = deliveryRepository.download(transferCode.code, keyPair)

					if (decryptedCertificates.isNotEmpty()) {
						decryptedCertificates.forEachIndexed { index, convertedCertificate ->
							val qrCodeData = convertedCertificate.qrCodeData
							val pdfData = convertedCertificate.pdfData
							if (index == 0) {
								walletDataStorage.replaceTransferCodeWithCertificate(transferCode, qrCodeData, pdfData)
								val decodeState = CovidCertificateSdk.Wallet.decode(qrCodeData)
								if (decodeState is DecodeState.SUCCESS) {
									conversionStateMutableLiveData.postValue(TransferCodeConversionState.CONVERTED(decodeState.certificateHolder))
								} else {
									// The certificate returned from the server could not be decoded
									conversionStateMutableLiveData.postValue(TransferCodeConversionState.NOT_CONVERTED)
								}
							} else {
								walletDataStorage.saveWalletDataItem(WalletDataItem.CertificateWalletData(qrCodeData, pdfData))
							}
							deleteTransferCodeOnServer(transferCode, keyPair)
						}
					} else {
						// The server returned no certificate
						conversionStateMutableLiveData.postValue(TransferCodeConversionState.NOT_CONVERTED)
					}
				} catch (e: TimeDeviationException) {
					conversionStateMutableLiveData.postValue(TransferCodeConversionState.ERROR(StateError(DeliveryRepository.ERROR_CODE_INVALID_TIME)))
				} catch (e: IOException) {
					// A request failed, check if the device has network connectivity or not
					if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
						conversionStateMutableLiveData.postValue(TransferCodeConversionState.ERROR(StateError(ErrorCodes.GENERAL_NETWORK_FAILURE)))
					} else {
						conversionStateMutableLiveData.postValue(TransferCodeConversionState.ERROR(StateError(ErrorCodes.GENERAL_OFFLINE)))
					}
				}
			} else {
				conversionStateMutableLiveData.postValue(TransferCodeConversionState.ERROR(StateError(ErrorCodes.INAPP_DELIVERY_KEYPAIR_GENERATION_FAILED)))
			}

			downloadJob = null
		}
	}

	fun removeTransferCode(transferCode: TransferCodeModel) {
		// Since the user shouldn't wait for this request to finish, the popBackStack is called shortly after calling this method.
		// In that case, the viewModelScope wouldn't make sense because it is cleared when the fragment is popped.
		// For this kind of fire-and-forget coroutine, the GlobalScope is therefore fine.
		GlobalScope.launch(Dispatchers.IO) {
			val keyPair = TransferCodeCrypto.loadKeyPair(transferCode.code, getApplication())
			if (keyPair != null) {
				deleteTransferCodeOnServer(transferCode, keyPair)
			}
		}
	}

	private suspend fun deleteTransferCodeOnServer(transferCode: TransferCodeModel, keyPair: KeyPair) {
		try {
			deliveryRepository.complete(transferCode.code, keyPair)
		} catch (e: IOException) {
			// This request is best-effort, if it fails, ignore it and let the backend delete the transfer code and certificate
			// automatically after it expires
		}
		TransferCodeCrypto.deleteKeyEntry(transferCode.code, getApplication())
	}

}