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
import androidx.lifecycle.*
import ch.admin.bag.covidcertificate.common.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder

class VerificationViewModel(application: Application) : AndroidViewModel(application) {

	private val certificateVerifier = CertificateVerifier(getApplication(), viewModelScope)
	val verificationLiveData: LiveData<VerificationState> = certificateVerifier.liveData

	fun startVerification(dccHolder: DccHolder) {
		certificateVerifier.startVerification(dccHolder)
	}

}