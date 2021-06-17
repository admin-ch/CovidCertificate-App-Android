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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeCreationState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

class TransferCodeCreationViewModel(application: Application) : AndroidViewModel(application) {

	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)

	private val creationStateMutableLiveData = MutableLiveData<TransferCodeCreationState>()
	val creationState = creationStateMutableLiveData as LiveData<TransferCodeCreationState>

	private var transferCodeCreationJob: Job? = null

	fun createTransferCode() {
		if (transferCodeCreationJob != null && transferCodeCreationJob?.isActive == true) return

		creationStateMutableLiveData.value = TransferCodeCreationState.LOADING
		transferCodeCreationJob = viewModelScope.launch(Dispatchers.Default) {
			delay(1000L)

			// TODO Generate public key, signature payload and signature and call backend endpoint
			val transferCode = TransferCodeModel("A2X56K7WP", Instant.now())
			walletDataStorage.saveWalletDataItem(WalletDataItem.TransferCodeWalletData(transferCode))
			creationStateMutableLiveData.postValue(TransferCodeCreationState.SUCCESS(transferCode))

			transferCodeCreationJob = null
		}
	}

}