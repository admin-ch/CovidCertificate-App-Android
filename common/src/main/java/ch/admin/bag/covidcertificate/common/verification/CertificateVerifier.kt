/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.verification

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import ch.admin.bag.covidcertificate.eval.*
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CertificateVerifier(
	private val context: Context,
	private val coroutineScope: CoroutineScope,
	private val dccHolder: DccHolder? = null,
) {

	private val checkSignatureStateMutableLiveData = MutableLiveData<CheckSignatureState>()
	private val checkRevocationStateMutableLiveData = MutableLiveData<CheckRevocationState>()
	private val checkNationalRulesStateMutableLiveData = MutableLiveData<CheckNationalRulesState>()

	private val stateMediator = MediatorLiveData<VerificationState>().apply {
		addSource(checkSignatureStateMutableLiveData) {
			val state =
				checkVerificationState(it, checkRevocationStateMutableLiveData.value, checkNationalRulesStateMutableLiveData.value)
			if (value != state) postValue(state)
		}
		addSource(checkRevocationStateMutableLiveData) {
			val state =
				checkVerificationState(checkSignatureStateMutableLiveData.value, it, checkNationalRulesStateMutableLiveData.value)
			if (value != state) postValue(state)
		}
		addSource(checkNationalRulesStateMutableLiveData) {
			val state =
				checkVerificationState(checkSignatureStateMutableLiveData.value, checkRevocationStateMutableLiveData.value, it)
			if (value != state) postValue(state)
		}
	}
	val liveData: LiveData<VerificationState> = stateMediator

	private fun checkVerificationState(
		checkSignatureState: CheckSignatureState?,
		checkRevocationState: CheckRevocationState?,
		checkNationalRulesState: CheckNationalRulesState?,
	): VerificationState {
		return if (checkSignatureState is CheckSignatureState.ERROR) {
			VerificationState.ERROR(
				checkSignatureState.error,
				{ checkSignatureState.error.dccHolder?.let { checkSignature(it) } },
				checkNationalRulesState?.validityRange()
			)
		} else if (checkRevocationState is CheckRevocationState.ERROR) {
			VerificationState.ERROR(
				checkRevocationState.error,
				{ checkRevocationState.error.dccHolder?.let { checkRevocationStatus(it) } },
				checkNationalRulesState?.validityRange()
			)
		} else if (checkNationalRulesState is CheckNationalRulesState.ERROR) {
			VerificationState.ERROR(
				checkNationalRulesState.error,
				{ checkNationalRulesState.error.dccHolder?.let { checkNationalRules(it) } },
				null
			)
		} else if (checkSignatureState == CheckSignatureState.SUCCESS &&
			checkRevocationState == CheckRevocationState.SUCCESS &&
			checkNationalRulesState is CheckNationalRulesState.SUCCESS
		)
			VerificationState.SUCCESS(checkNationalRulesState.validityRange)
		else if (checkSignatureState is CheckSignatureState.INVALID ||
			checkRevocationState is CheckRevocationState.INVALID ||
			checkNationalRulesState is CheckNationalRulesState.INVALID ||
			checkNationalRulesState is CheckNationalRulesState.NOT_YET_VALID ||
			checkNationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE
		) {
			VerificationState.INVALID(
				checkSignatureState, checkRevocationState, checkNationalRulesState,
				checkNationalRulesState?.validityRange()
			)
		} else {
			VerificationState.LOADING
		}
	}

	private fun checkSignature(dccHolder: DccHolder) {
		coroutineScope.launch(Dispatchers.IO) {
			try {
				val checkSignatureState = Eval.checkSignature(dccHolder, context)
				checkSignatureStateMutableLiveData.postValue(checkSignatureState)
			} catch (e: Exception) {
				checkSignatureStateMutableLiveData.postValue(
					CheckSignatureState.ERROR(Error(EvalErrorCodes.SIGNATURE_UNKNOWN, e.message.toString(), dccHolder))
				)
			}
		}
	}

	private fun checkRevocationStatus(dccHolder: DccHolder) {
		coroutineScope.launch(Dispatchers.IO) {
			try {
				val checkRevocationState = Eval.checkRevocationStatus(dccHolder, context)
				checkRevocationStateMutableLiveData.postValue(checkRevocationState)
			} catch (e: Exception) {
				checkRevocationStateMutableLiveData.postValue(
					CheckRevocationState.ERROR(Error(EvalErrorCodes.REVOCATION_UNKNOWN, e.message.toString(), dccHolder))
				)
			}
		}
	}

	private fun checkNationalRules(dccHolder: DccHolder) {
		coroutineScope.launch(Dispatchers.IO) {
			try {
				val checkNationalRulesState = Eval.checkNationalRules(dccHolder, context)
				checkNationalRulesStateMutableLiveData.postValue(checkNationalRulesState)
			} catch (e: Exception) {
				checkNationalRulesStateMutableLiveData.postValue(
					CheckNationalRulesState.ERROR(Error(EvalErrorCodes.RULESET_UNKNOWN, e.message.toString(), dccHolder))
				)
			}
		}
	}

	fun startVerification(dccHolder: DccHolder? = this.dccHolder, delay: Long = 0) {
		val dccHolder = dccHolder ?: return
		stateMediator.value = VerificationState.LOADING
		coroutineScope.launch(Dispatchers.IO) {
			if (delay > 0) delay(delay)
			checkSignature(dccHolder)
			checkRevocationStatus(dccHolder)
			checkNationalRules(dccHolder)
		}

	}
}