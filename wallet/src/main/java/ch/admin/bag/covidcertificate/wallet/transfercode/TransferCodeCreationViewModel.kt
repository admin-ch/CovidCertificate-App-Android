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
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.Luhn
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.TransferCodeCrypto
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeCreationResponse
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeCreationState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliveryRepository
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliverySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.KeyPair
import java.time.Instant

class TransferCodeCreationViewModel(application: Application) : AndroidViewModel(application) {

	companion object {
		const val ERROR_CODE_INVALID_TIME = "I|TIME425"
	}

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val deliveryRepository = DeliveryRepository.getInstance(DeliverySpec(application.applicationContext, BuildConfig.BASE_URL_DELIVERY))
	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)

	private val creationStateMutableLiveData = MutableLiveData<TransferCodeCreationState>()
	val creationState = creationStateMutableLiveData as LiveData<TransferCodeCreationState>

	private var transferCodeCreationJob: Job? = null

	fun createAndRegisterTransferCode() {
		if (transferCodeCreationJob != null && transferCodeCreationJob?.isActive == true) return

		creationStateMutableLiveData.value = TransferCodeCreationState.LOADING
		transferCodeCreationJob = viewModelScope.launch(Dispatchers.Default) {
			val transferCode = Luhn.generateNewTransferCode()
			val keyPair = TransferCodeCrypto.createKeyPair(transferCode, getApplication())

			if (keyPair != null) {
				registerTransferCode(transferCode, keyPair)
			} else {
				creationStateMutableLiveData.postValue(TransferCodeCreationState.ERROR(StateError(ErrorCodes.INAPP_DELIVERY_KEYPAIR_GENERATION_FAILED)))
			}

			transferCodeCreationJob = null
		}
	}

	private suspend fun registerTransferCode(transferCode: String, keyPair: KeyPair) = withContext(Dispatchers.IO) {
		try {
			val registrationResponse = deliveryRepository.register(transferCode, keyPair)
			when (registrationResponse) {
				TransferCodeCreationResponse.SUCCESSFUL -> {
					val now = Instant.now()
					val transferCodeModel = TransferCodeModel(transferCode, now, now)
					walletDataStorage.saveWalletDataItem(WalletDataItem.TransferCodeWalletData(transferCodeModel))
					creationStateMutableLiveData.postValue(TransferCodeCreationState.SUCCESS(transferCodeModel))
				}
				TransferCodeCreationResponse.INVALID_TIME -> {
					creationStateMutableLiveData.postValue(TransferCodeCreationState.ERROR(StateError(ERROR_CODE_INVALID_TIME)))
				}
				else -> {
					creationStateMutableLiveData.postValue(TransferCodeCreationState.ERROR(StateError(ErrorCodes.INAPP_DELIVERY_REGISTRATION_FAILED)))
				}
			}
		} catch (e: IOException) {
			if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
				creationStateMutableLiveData.postValue(TransferCodeCreationState.ERROR(StateError(ErrorCodes.GENERAL_NETWORK_FAILURE)))
			} else {
				creationStateMutableLiveData.postValue(TransferCodeCreationState.ERROR(StateError(ErrorCodes.GENERAL_OFFLINE)))
			}
		}
	}

}