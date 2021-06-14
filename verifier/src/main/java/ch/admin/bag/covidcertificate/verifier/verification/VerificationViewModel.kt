/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.verification

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerificationTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VerificationViewModel(application: Application) : AndroidViewModel(application) {

	companion object {
		private const val STATUS_LOAD_DELAY = 1000L
	}

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val verificationController = CovidCertificateSdk.getCertificateVerificationController()
	private val verificationStateMutableLiveData = MutableLiveData<VerificationState>()
	val verificationLiveData = verificationStateMutableLiveData as LiveData<VerificationState>

	fun startVerification(dccHolder: DccHolder) {
		val task = CertificateVerificationTask(dccHolder, connectivityManager)

		viewModelScope.launch {
			task.verificationStateFlow.collect {
				verificationStateMutableLiveData.postValue(it)
			}
		}

		verificationController.enqueue(task, viewModelScope)
	}

	fun retryVerification(dccHolder: DccHolder?) {
		verificationStateMutableLiveData.value = VerificationState.LOADING
		viewModelScope.launch {
			delay(STATUS_LOAD_DELAY)
			if (!isActive) return@launch

			dccHolder?.let { startVerification(it) }
		}
	}

}