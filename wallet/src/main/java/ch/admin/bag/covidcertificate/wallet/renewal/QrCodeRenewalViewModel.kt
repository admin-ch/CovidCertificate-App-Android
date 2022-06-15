/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.renewal

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.renewal.model.QrCodeRenewalResponse
import ch.admin.bag.covidcertificate.wallet.renewal.model.QrCodeRenewalViewState
import ch.admin.bag.covidcertificate.wallet.renewal.net.QrCodeRenewalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class QrCodeRenewalViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)
	private val repository = QrCodeRenewalRepository.getInstance(application.applicationContext)

	private val viewStateMutable = MutableStateFlow<QrCodeRenewalViewState>(QrCodeRenewalViewState.RenewalRequired)
	val viewState = viewStateMutable.asStateFlow()

	fun setCertificate(certificateHolder: CertificateHolder) {
		val wasRenewedRecently = walletDataStorage.wasCertificateRecentlyRenewed(certificateHolder)
		viewStateMutable.value = if (wasRenewedRecently) {
			QrCodeRenewalViewState.RenewalSuccessful
		} else {
			QrCodeRenewalViewState.RenewalRequired
		}
	}

	fun renewCertificate(certificateHolder: CertificateHolder) {
		viewStateMutable.value = QrCodeRenewalViewState.RenewalInProgress

		viewModelScope.launch(Dispatchers.IO) {
			try {
				val renewalResponse = repository.renew(certificateHolder)
				viewStateMutable.value = when (renewalResponse) {
					is QrCodeRenewalResponse.Success -> {
						val newQrCodeData = renewalResponse.hcert
						walletDataStorage.updateCertificateQrCodeData(certificateHolder, newQrCodeData)

						QrCodeRenewalViewState.RenewalSuccessful
					}
					is QrCodeRenewalResponse.RateLimitExceeded -> {
						QrCodeRenewalViewState.RenewalFailed(StateError(QrCodeRenewalErrorCodes.RATE_LIMIT_EXCEEDED))
					}
					is QrCodeRenewalResponse.Failed -> {
						val error = getNetworkStateError()
						QrCodeRenewalViewState.RenewalFailed(error)
					}
				}
			} catch (e: IOException) {
				val error = getNetworkStateError()
				viewStateMutable.value = QrCodeRenewalViewState.RenewalFailed(error)
			}
		}
	}

	private fun getNetworkStateError(): StateError {
		return if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
			StateError(ErrorCodes.GENERAL_NETWORK_FAILURE)
		} else {
			StateError(ErrorCodes.GENERAL_OFFLINE)
		}
	}

}