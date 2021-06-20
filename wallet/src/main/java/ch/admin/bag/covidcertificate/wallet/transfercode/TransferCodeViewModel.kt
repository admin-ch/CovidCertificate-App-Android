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
import ch.admin.bag.covidcertificate.eval.utils.NetworkUtil
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.TransferCodeCrypto
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliveryRepository
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliverySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class TransferCodeViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)
	private val deliveryRepository = DeliveryRepository.getInstance(DeliverySpec(application.applicationContext, BuildConfig.BASE_URL_DELIVERY))

	private var downloadJob: Job? = null

	fun downloadCertificateForTransferCode(transferCode: TransferCodeModel) {
		downloadJob?.cancel()

		downloadJob = viewModelScope.launch(Dispatchers.IO) {
			val keyPair = TransferCodeCrypto.loadKeyPair(transferCode.code)

			if (keyPair != null) {
				try {
					val certificateQrCodeData = deliveryRepository.download(transferCode.code, keyPair)

					if (certificateQrCodeData != null) {
						walletDataStorage.replaceTransferCodeWithCertificate(transferCode, certificateQrCodeData)

						// TODO Call complete in the backend
					} else {
						// TODO Show error
					}
				} catch (e: IOException) {
					if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
						// TODO Post general network failure error
					} else {
						// TODO Post offline error
					}
				}
			} else {
				// TODO Show error
			}

			downloadJob = null
		}
	}

}