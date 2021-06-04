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
import androidx.lifecycle.*
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.util.SingleLiveEvent
import ch.admin.bag.covidcertificate.common.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.DecodeState
import ch.admin.bag.covidcertificate.eval.Eval
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.data.CertificateStorage
import ch.admin.bag.covidcertificate.wallet.networking.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CertificatesViewModel(application: Application) : AndroidViewModel(application) {

	private val dccHolderCollectionMutableLiveData = MutableLiveData<List<DccHolder>>()
	val dccHolderCollectionLiveData: LiveData<List<DccHolder>> = dccHolderCollectionMutableLiveData

	private val certificateStorage: CertificateStorage by lazy { CertificateStorage.getInstance(getApplication()) }

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<DccHolder>()

	fun loadCertificates() {
		viewModelScope.launch(Dispatchers.Default) {
			dccHolderCollectionMutableLiveData.postValue(
				certificateStorage.getCertificateList().mapNotNull { (Eval.decode(it) as? DecodeState.SUCCESS)?.dccHolder }
			)
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
		val configRepository = ConfigRepository.getInstance(getApplication())
		viewModelScope.launch {
			configRepository.loadConfig(getApplication())?.let { config -> configMutableLiveData.postValue(config) }
		}
	}

	private val certificateVerifierMapMutableLiveData = MutableLiveData<Map<String, CertificateVerifier>>(HashMap())
	val certificateVerifierMapLiveData: LiveData<Map<String, CertificateVerifier>> = certificateVerifierMapMutableLiveData

	private val verifiedCertificatesMediatorLiveData = MediatorLiveData<List<VerifiedCertificate>>().apply {
		addSource(dccHolderCollectionLiveData) { certificates ->
			value =
				certificates.map { certificate ->
					val verifier = certificateVerifierMapLiveData.value?.get(certificate.qrCodeData)
					val state = verifier?.liveData?.value
					return@map VerifiedCertificate(certificate, state ?: VerificationState.LOADING)
				}
		}
	}
	val verifiedCertificatesLiveData: LiveData<List<VerifiedCertificate>> = verifiedCertificatesMediatorLiveData

	init {
		dccHolderCollectionLiveData.observeForever { certificates -> updateCertificateVerifiers(certificates) }
	}

	private fun updateCertificateVerifiers(dccHolders: List<DccHolder>) {
		val certificateVerifierMap = certificateVerifierMapLiveData.value!!.toMutableMap()
		val newCertificateSet = dccHolders.map { bagdgc -> bagdgc.qrCodeData }.toSet()

		certificateVerifierMap.keys
			.filter { !newCertificateSet.contains(it) }
			.forEach { removedCertificate ->
				certificateVerifierMap[removedCertificate]?.liveData?.let { verifiedCertificatesMediatorLiveData.removeSource(it) }
				certificateVerifierMap.remove(removedCertificate)
			}

		dccHolders
			.filter { !certificateVerifierMap.containsKey(it.qrCodeData) }
			.forEach { addedCertificate ->
				val newVerifier = CertificateVerifier(getApplication(), viewModelScope, addedCertificate)
				certificateVerifierMap[addedCertificate.qrCodeData] = newVerifier
				verifiedCertificatesMediatorLiveData.addSource(newVerifier.liveData) { state ->
					val currentStates = verifiedCertificatesMediatorLiveData.value ?: return@addSource
					verifiedCertificatesMediatorLiveData.value = currentStates.map { verifiedCertificate ->
						return@map if (verifiedCertificate.dccHolder.qrCodeData == addedCertificate.qrCodeData) {
							VerifiedCertificate(addedCertificate, state)
						} else {
							verifiedCertificate
						}
					}
				}
			}
		certificateVerifierMapMutableLiveData.value = certificateVerifierMap
	}

	data class VerifiedCertificate(val dccHolder: DccHolder, val state: VerificationState)
}