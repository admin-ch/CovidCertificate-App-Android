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
import ch.admin.bag.covidcertificate.common.util.SingleLiveEvent
import ch.admin.bag.covidcertificate.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.verification.CertificateVerificationTask
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.WalletItem
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
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

	private val walletDataStorage: WalletDataSecureStorage by lazy { WalletDataSecureStorage.getInstance(application.applicationContext) }

	private val walletItemsMutableLiveData = MutableLiveData<List<WalletItem>>()
	val walletItems = walletItemsMutableLiveData as LiveData<List<WalletItem>>

	private val verifiedCertificatesMutableLiveData = MutableLiveData<List<VerifiedCertificate>>()
	val verifiedCertificates: LiveData<List<VerifiedCertificate>> = verifiedCertificatesMutableLiveData
	private val verificationJobs = mutableMapOf<DccHolder, Job>()

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<DccHolder>()
	val onTransferCodeClickedSingleLiveEvent = SingleLiveEvent<TransferCodeModel>()

	init {
		walletItems.observeForever { items ->
			// Filter only dcc holders
			val certificates = items.filterIsInstance<WalletItem.DccHolderItem>()

			// When the stored DccHolders change, map the verified certificates with the existing verification state or LOADING
			val currentVerifiedCertificates = verifiedCertificates.value ?: emptyList()
			verifiedCertificatesMutableLiveData.value = certificates.map { certificate ->
				currentVerifiedCertificates.find {
					it.dccHolder == certificate.dccHolder
				} ?: VerifiedCertificate(certificate.dccHolder, VerificationState.LOADING)
			}

			// (Re-)Verify all certificates
			certificates.forEach { startVerification(it.dccHolder) }
		}
	}

	fun loadWalletData() {
		viewModelScope.launch(Dispatchers.Default) {
			val pagerHolders = walletDataStorage.getWalletData().mapNotNull { dataItem ->
				when (dataItem) {
					is WalletDataItem.CertificateWalletData -> (CertificateDecoder.decode(dataItem.qrCodeData) as? DecodeState.SUCCESS)?.let {
						WalletItem.DccHolderItem(
							it.dccHolder.qrCodeData.hashCode(),
							it.dccHolder
						)
					}
					is WalletDataItem.TransferCodeWalletData -> WalletItem.TransferCodeHolderItem(
						dataItem.transferCode.hashCode(),
						dataItem.transferCode
					)
				}
			}

			walletItemsMutableLiveData.postValue(pagerHolders)
		}
	}

	fun startVerification(dccHolder: DccHolder, delayInMillis: Long = 0L, isForceVerification: Boolean = false) {
		if (isForceVerification) {
			verificationController.refreshTrustList(viewModelScope)
		}

		verificationJobs[dccHolder]?.cancel()

		val task = CertificateVerificationTask(dccHolder, connectivityManager)
		val job = viewModelScope.launch {
			task.verificationStateFlow.collect { state ->
				// Replace the verified certificate in the live data
				val newVerifiedCertificates = verifiedCertificates.value?.toMutableList() ?: mutableListOf()
				val index = newVerifiedCertificates.indexOfFirst { it.dccHolder == dccHolder }
				if (index >= 0) {
					newVerifiedCertificates[index] = VerifiedCertificate(dccHolder, state)
				} else {
					newVerifiedCertificates.add(VerifiedCertificate(dccHolder, state))
				}

				// Set the livedata value on the main dispatcher to prevent multiple posts overriding each other
				withContext(Dispatchers.Main.immediate) {
					verifiedCertificatesMutableLiveData.value = newVerifiedCertificates
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

	fun onQrCodeClicked(dccHolder: DccHolder) {
		onQrCodeClickedSingleLiveEvent.postValue(dccHolder)
	}

	fun onTransferCodeClicked(transferCode: TransferCodeModel) {
		onTransferCodeClickedSingleLiveEvent.postValue(transferCode)
	}

	fun containsCertificate(certificate: String): Boolean {
		return walletDataStorage.containsCertificate(certificate)
	}

	fun addCertificate(certificate: String) {
		val item = WalletDataItem.CertificateWalletData(certificate)
		walletDataStorage.saveWalletDataItem(item)
	}

	fun moveWalletDataItem(from: Int, to: Int) {
		walletDataStorage.changeWalletDataItemPosition(from, to)
	}

	fun removeCertificate(certificate: String) {
		walletDataStorage.deleteCertificate(certificate)
		loadWalletData()
	}

	data class VerifiedCertificate(val dccHolder: DccHolder, val state: VerificationState)
}