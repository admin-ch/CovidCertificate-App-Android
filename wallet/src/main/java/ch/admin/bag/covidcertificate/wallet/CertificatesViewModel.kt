/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.net.ConfigSpec
import ch.admin.bag.covidcertificate.common.util.SingleLiveEvent
import ch.admin.bag.covidcertificate.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerificationTask
import ch.admin.bag.covidcertificate.wallet.data.CertificateStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.set

class CertificatesViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val verificationController = CovidCertificateSdk.getCertificateVerificationController()

	private val dccHolderCollectionMutableLiveData = MutableLiveData<List<DccHolder>>()
	val dccHolderCollectionLiveData: LiveData<List<DccHolder>> = dccHolderCollectionMutableLiveData

	private val verifiedCertificatesMutableLiveData = MutableLiveData<List<VerifiedCertificate>>()
	val verifiedCertificates: LiveData<List<VerifiedCertificate>> = verifiedCertificatesMutableLiveData
	private val verificationJobs = mutableMapOf<DccHolder, Job>()

	private val certificateStorage: CertificateStorage by lazy { CertificateStorage.getInstance(getApplication()) }

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<DccHolder>()

	init {
		dccHolderCollectionLiveData.observeForever { certificates ->
			// When the stored DccHolders change, map the verified certificates with the existing verification state or LOADING
			val currentVerifiedCertificates = verifiedCertificates.value ?: emptyList()
			verifiedCertificatesMutableLiveData.value = certificates.map { certificate ->
				currentVerifiedCertificates.find { it.dccHolder == certificate } ?: VerifiedCertificate(certificate, VerificationState.LOADING)
			}

			// (Re-)Verify all certificates
			certificates.forEach { startVerification(it) }
		}
	}

	fun loadCertificates() {
		viewModelScope.launch(Dispatchers.Default) {
			dccHolderCollectionMutableLiveData.postValue(
				certificateStorage.getCertificateList().mapNotNull { (CertificateDecoder.decode(it) as? DecodeState.SUCCESS)?.dccHolder }
			)
		}
	}

	fun startVerification(dccHolder: DccHolder, delayInMillis: Long = 0L, isForceVerification: Boolean = false) {
		if (isForceVerification) {
			// Manually show the loading state for this certificate. This would be done by the verification task,
			// but since we first load the trust list, that happens too late in the UI
			val verifiedCertificatesWithLoading = updateVerificationStateForDccHolder(dccHolder, VerificationState.LOADING)
			verifiedCertificatesMutableLiveData.value = verifiedCertificatesWithLoading

			// If this is a force verification (from the detail page), frist refresh the trust list
			verificationController.refreshTrustList(viewModelScope, onCompletionCallback = {
				val task = CertificateVerificationTask(dccHolder, connectivityManager)
				enqueueVerificationTask(task, delayInMillis)
			}, onErrorCallback = {
				// If loading the trust list failed, tell the verification task to ignore the local trust list.
				// That way the offline mode / network failure error handling is already taken care of by the verification controller
				val task = CertificateVerificationTask(dccHolder, connectivityManager, ignoreLocalTrustList = true)
				enqueueVerificationTask(task, delayInMillis)
			})
		} else {
			val task = CertificateVerificationTask(dccHolder, connectivityManager)
			enqueueVerificationTask(task, delayInMillis)
		}
	}

	fun onQrCodeClicked(dccHolder: DccHolder) {
		onQrCodeClickedSingleLiveEvent.postValue(dccHolder)
	}

	fun containsCertificate(certificate: String): Boolean {
		return certificateStorage.containsCertificate(certificate)
	}

	fun addCertificate(certificate: String) {
		certificateStorage.saveCertificate(certificate)
	}

	fun moveCertificate(from: Int, to: Int) {
		certificateStorage.changeCertificatePosition(from, to)
	}

	fun removeCertificate(certificate: String) {
		certificateStorage.deleteCertificate(certificate)
		loadCertificates()
	}

	private val configMutableLiveData = MutableLiveData<ConfigModel>()
	val configLiveData: LiveData<ConfigModel> = configMutableLiveData

	fun loadConfig() {
		val configRepository = ConfigRepository.getInstance(ConfigSpec(getApplication(),
			BuildConfig.BASE_URL,
			BuildConfig.VERSION_NAME,
			BuildConfig.BUILD_TIME.toString()))
		viewModelScope.launch {
			configRepository.loadConfig(getApplication())?.let { config -> configMutableLiveData.postValue(config) }
		}
	}

	private fun enqueueVerificationTask(task: CertificateVerificationTask, delayInMillis: Long) {
		val dccHolder = task.dccHolder
		verificationJobs[dccHolder]?.cancel()

		// Wait for above refresh
		val job = viewModelScope.launch {
			task.verificationStateFlow.collect { state ->
				// Replace the verified certificate in the live data
				val updatedVerifiedCertificates = updateVerificationStateForDccHolder(dccHolder, state)

				// Set the livedata value on the main dispatcher to prevent multiple posts overriding each other
				withContext(Dispatchers.Main.immediate) {
					verifiedCertificatesMutableLiveData.value = updatedVerifiedCertificates
				}

				// Once the verification state is not loading anymore, cancel the flow collection job (otherwise the flow stays active without emitting anything)
				if (state !is VerificationState.LOADING) {
					verificationJobs[dccHolder]?.cancel()
					verificationJobs.remove(dccHolder)
				}
			}
		}
		verificationJobs[dccHolder] = job

		viewModelScope.launch {
			if (delayInMillis > 0) delay(delayInMillis)
			verificationController.enqueue(task, viewModelScope)
		}
	}

	private fun updateVerificationStateForDccHolder(
		dccHolder: DccHolder,
		newVerificationState: VerificationState
	): List<VerifiedCertificate> {
		val newVerifiedCertificates = verifiedCertificates.value?.toMutableList() ?: mutableListOf()
		val index = newVerifiedCertificates.indexOfFirst { it.dccHolder == dccHolder }
		if (index >= 0) {
			newVerifiedCertificates[index] = VerifiedCertificate(dccHolder, newVerificationState)
		} else {
			newVerifiedCertificates.add(VerifiedCertificate(dccHolder, newVerificationState))
		}

		return newVerifiedCertificates
	}

	data class VerifiedCertificate(val dccHolder: DccHolder, val state: VerificationState)
}