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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.util.SingleLiveEvent
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
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
import java.time.Instant
import kotlin.collections.set

class CertificatesViewModel(application: Application) : AndroidViewModel(application) {

	private val walletDataStorage: WalletDataSecureStorage by lazy { WalletDataSecureStorage.getInstance(application.applicationContext) }

	private val walletItemsMutableLiveData = MutableLiveData<List<WalletItem>>()
	val walletItems = walletItemsMutableLiveData as LiveData<List<WalletItem>>

	private val verifiedCertificatesMutableLiveData = MutableLiveData<List<VerifiedCertificate>>()
	val verifiedCertificates: LiveData<List<VerifiedCertificate>> = verifiedCertificatesMutableLiveData
	private val verificationJobs = mutableMapOf<CertificateHolder, Job>()

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<CertificateHolder>()
	val onCertificateLightClickedSingleLiveEvent = SingleLiveEvent<Pair<String, CertificateHolder>>()
	val onTransferCodeClickedSingleLiveEvent = SingleLiveEvent<TransferCodeModel>()

	init {
		walletItems.observeForever { items ->
			// When the wallet items change, map the certificates with the existing verification state or LOADING
			val certificateItems = items.filterIsInstance<WalletItem.CertificateHolderItem>()
			val currentVerifiedCertificates = verifiedCertificates.value ?: emptyList()
			verifiedCertificatesMutableLiveData.value = certificateItems.map { item ->
				currentVerifiedCertificates.find {
					it.qrCodeData == item.qrCodeData
				} ?: VerifiedCertificate(item.qrCodeData, item.certificateHolder, VerificationState.LOADING)
			}

			// (Re-)Verify all certificates
			certificateItems.forEach { item -> item.certificateHolder?.let { startVerification(it) } }
		}
	}

	fun loadWalletData() {
		viewModelScope.launch(Dispatchers.Default) {
			val pagerHolders = walletDataStorage.getWalletData().map { dataItem ->
				when (dataItem) {
					is WalletDataItem.CertificateWalletData -> {
						var holderItem: WalletItem.CertificateHolderItem? = null
						if (dataItem.certificateLightData != null && dataItem.certificateLightQrCode != null) {
							// If the wallet data item contains a certificate light, decode it to check it's expiration timestamp
							val certificateLightDecodeState = CovidCertificateSdk.Wallet.decode(dataItem.certificateLightData)
							if (
								certificateLightDecodeState is DecodeState.SUCCESS
								&& certificateLightDecodeState.certificateHolder.expirationTime?.isAfter(Instant.now()) == true
							) {
								holderItem = WalletItem.CertificateHolderItem(
									dataItem.certificateLightData.hashCode(),
									dataItem.certificateLightData,
									dataItem.certificateLightQrCode,
									certificateLightDecodeState.certificateHolder
								)
							} else {
								// Remove the certificate light data if it expired or could not be decoded
								walletDataStorage.deleteCertificateLight(dataItem.qrCodeData)
							}
						}

						if (holderItem == null) {
							// If the wallet data item didn't contain a certificate light or it already expired, decode the regular
							// certificate and map it to a pager holder item
							val decodeState = CovidCertificateSdk.Wallet.decode(dataItem.qrCodeData)
							if (decodeState is DecodeState.ERROR) {
								verifiedCertificatesMutableLiveData.postValue(
									(verifiedCertificates.value?.toMutableList() ?: mutableListOf()).apply {
										add(
											VerifiedCertificate(
												dataItem.qrCodeData,
												null,
												VerificationState.ERROR(decodeState.error, null)
											)
										)
									})
							}

							holderItem = WalletItem.CertificateHolderItem(
								dataItem.qrCodeData.hashCode(),
								dataItem.qrCodeData,
								null,
								(decodeState as? DecodeState.SUCCESS?)?.certificateHolder
							)
						}

						holderItem
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

	fun startVerification(certificateHolder: CertificateHolder, delayInMillis: Long = 0L, isForceVerification: Boolean = false) {
		if (isForceVerification) {
			// Manually show the loading state for this certificate. This would be done by the verification task,
			// but since we first load the trust list, that happens too late in the UI
			val verifiedCertificatesWithLoading = updateVerificationStateForDccHolder(certificateHolder, VerificationState.LOADING)
			verifiedCertificatesMutableLiveData.value = verifiedCertificatesWithLoading

			// If this is a force verification (from the detail page), frist refresh the trust list
			CovidCertificateSdk.refreshTrustList(viewModelScope, onCompletionCallback = {
				enqueueVerificationTask(certificateHolder, delayInMillis)
			}, onErrorCallback = {
				// If loading the trust list failed, tell the verification task to ignore the local trust list.
				// That way the offline mode / network failure error handling is already taken care of by the verification controller
				enqueueVerificationTask(certificateHolder, delayInMillis, ignoreLocalTrustList = true)
			})
		} else {
			enqueueVerificationTask(certificateHolder, delayInMillis)
		}
	}

	fun onQrCodeClicked(certificateHolder: CertificateHolder) {
		onQrCodeClickedSingleLiveEvent.postValue(certificateHolder)
	}

	fun onCertificateLightClicked(qrCodeImage: String, certificateHolder: CertificateHolder) {
		onCertificateLightClickedSingleLiveEvent.postValue(qrCodeImage to certificateHolder)
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

	fun updateTransferCodeLastUpdated(transferCode: TransferCodeModel): TransferCodeModel {
		return walletDataStorage.updateTransferCodeLastUpdated(transferCode)
	}

	fun moveWalletDataItem(from: Int, to: Int) {
		walletDataStorage.changeWalletDataItemPosition(from, to)
	}

	fun removeCertificate(certificate: String) {
		walletDataStorage.deleteCertificate(certificate)
		loadWalletData()
	}

	fun removeTransferCode(transferCode: TransferCodeModel) {
		walletDataStorage.deleteTransferCode(transferCode)
		loadWalletData()
	}

	private fun enqueueVerificationTask(
		certificateHolder: CertificateHolder,
		delayInMillis: Long,
		ignoreLocalTrustList: Boolean = false
	) {
		verificationJobs[certificateHolder]?.cancel()

		viewModelScope.launch {
			if (delayInMillis > 0) delay(delayInMillis)
			val verificationStateFlow = CovidCertificateSdk.Wallet.verify(certificateHolder, viewModelScope, ignoreLocalTrustList)

			val job = viewModelScope.launch {
				verificationStateFlow.collect { state ->
					// Replace the verified certificate in the live data
					val updatedVerifiedCertificates = updateVerificationStateForDccHolder(certificateHolder, state)

					// Set the livedata value on the main dispatcher to prevent multiple posts overriding each other
					withContext(Dispatchers.Main.immediate) {
						verifiedCertificatesMutableLiveData.value = updatedVerifiedCertificates
					}

					// Once the verification state is not loading anymore, cancel the flow collection job (otherwise the flow stays active without emitting anything)
					if (state !is VerificationState.LOADING) {
						verificationJobs[certificateHolder]?.cancel()
						verificationJobs.remove(certificateHolder)
					}
				}
			}
			verificationJobs[certificateHolder] = job
		}
	}

	private fun updateVerificationStateForDccHolder(
		certificateHolder: CertificateHolder,
		newVerificationState: VerificationState
	): List<VerifiedCertificate> {
		val newVerifiedCertificates = verifiedCertificates.value?.toMutableList() ?: mutableListOf()
		val index = newVerifiedCertificates.indexOfFirst { it.certificateHolder == certificateHolder }
		if (index >= 0) {
			newVerifiedCertificates[index] =
				VerifiedCertificate(certificateHolder.qrCodeData, certificateHolder, newVerificationState)
		} else {
			newVerifiedCertificates.add(VerifiedCertificate(certificateHolder.qrCodeData, certificateHolder, newVerificationState))
		}

		return newVerifiedCertificates
	}

	data class VerifiedCertificate(val qrCodeData: String, val certificateHolder: CertificateHolder?, val state: VerificationState)
}