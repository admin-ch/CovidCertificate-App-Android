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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.exception.TimeDeviationException
import ch.admin.bag.covidcertificate.common.util.SingleLiveEvent
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.WalletItem
import ch.admin.bag.covidcertificate.wallet.transfercode.TransferCodeErrorCodes
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.TransferCodeCrypto
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliveryRepository
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliverySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import kotlin.collections.set

class CertificatesAndConfigViewModel(application: Application) : ConfigViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val walletDataStorage: WalletDataSecureStorage by lazy { WalletDataSecureStorage.getInstance(application.applicationContext) }
	private val deliveryRepository =
		DeliveryRepository.getInstance(DeliverySpec(application.applicationContext, BuildConfig.BASE_URL_DELIVERY))

	private val walletItemsMutableLiveData = MutableLiveData<List<WalletItem>>()
	val walletItems = walletItemsMutableLiveData as LiveData<List<WalletItem>>

	private val statefulWalletItemsMutableLiveData = MutableLiveData<List<StatefulWalletItem>>()
	val statefulWalletItems: LiveData<List<StatefulWalletItem>> = statefulWalletItemsMutableLiveData
	private val verificationJobs = mutableMapOf<CertificateHolder, Job>()

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<CertificateHolder>()
	val onCertificateLightClickedSingleLiveEvent = SingleLiveEvent<Pair<String, CertificateHolder>>()
	val onTransferCodeClickedSingleLiveEvent = SingleLiveEvent<TransferCodeModel>()

	init {
		walletItems.observeForever { items ->
			// When the wallet items change, map the certificates and transfer codes with the existing verification/conversion state or LOADING
			val currentStatefulWalletItems = statefulWalletItems.value ?: emptyList()
			statefulWalletItemsMutableLiveData.value = items.map { item ->
				when (item) {
					is WalletItem.CertificateHolderItem -> {
						currentStatefulWalletItems.find {
							it is StatefulWalletItem.VerifiedCertificate && it.qrCodeData == item.qrCodeData
						} ?: StatefulWalletItem.VerifiedCertificate(
							item.qrCodeData,
							item.certificateHolder,
							VerificationState.LOADING
						)
					}
					is WalletItem.TransferCodeHolderItem -> {
						currentStatefulWalletItems.find {
							it is StatefulWalletItem.TransferCodeConversionItem && it.transferCode.code == item.transferCode.code
						} ?: StatefulWalletItem.TransferCodeConversionItem(item.transferCode, TransferCodeConversionState.LOADING)
					}
				}
			}

			// (Re-)Verify certificates and try to convert transfer codes
			items.forEach { item ->
				when (item) {
					is WalletItem.CertificateHolderItem -> item.certificateHolder?.let { startVerification(it) }
					is WalletItem.TransferCodeHolderItem -> convertTransferCode(item.transferCode)
				}
			}
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
								statefulWalletItemsMutableLiveData.postValue(
									(statefulWalletItems.value?.toMutableList() ?: mutableListOf()).apply {
										add(
											StatefulWalletItem.VerifiedCertificate(
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
			val updatedStatefulWalletItems = updateVerificationStateForDccHolder(certificateHolder, VerificationState.LOADING)
			statefulWalletItemsMutableLiveData.value = updatedStatefulWalletItems

			// If this is a force verification (from the detail page), first refresh the trust list
			CovidCertificateSdk.refreshTrustList(
				viewModelScope,
				onCompletionCallback = {
					enqueueVerificationTask(certificateHolder, delayInMillis)
				},
				onErrorCallback = { errorCode ->
					statefulWalletItemsMutableLiveData.value = updateVerificationStateForDccHolder(
						certificateHolder,
						VerificationState.ERROR(StateError(errorCode), null)
					)
				}
			)
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
		delayInMillis: Long
	) {
		verificationJobs[certificateHolder]?.cancel()

		viewModelScope.launch {
			if (delayInMillis > 0) delay(delayInMillis)
			val verificationIdentifier = CovidCertificateSdk.Wallet.getActiveModes().value.map { activeMode -> activeMode.id }.toSet()
			val verificationStateFlow =
				CovidCertificateSdk.Wallet.verify(certificateHolder, verificationIdentifier, viewModelScope)
			val job = viewModelScope.launch {
				verificationStateFlow.collect { state ->
					// Replace the verified certificate in the live data
					val updatedStatefulWalletItems = updateVerificationStateForDccHolder(certificateHolder, state)

					// Set the livedata value on the main dispatcher to prevent multiple posts overriding each other
					withContext(Dispatchers.Main.immediate) {
						statefulWalletItemsMutableLiveData.value = updatedStatefulWalletItems
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

	private fun convertTransferCode(transferCode: TransferCodeModel) {
		if (transferCode.isFailed()) {
			// If transfer code is failed, return early with the error code for a failed transfer code
			val conversionState = TransferCodeConversionState.ERROR(StateError(DeliveryRepository.ERROR_CODE_FAILED))
			val updatedStatefulWalletItems = updateConversionStateForTransferCode(transferCode, conversionState)
			statefulWalletItemsMutableLiveData.value = updatedStatefulWalletItems
			return
		}

		viewModelScope.launch(Dispatchers.IO) {
			TransferCodeCrypto.getMutex(transferCode.code).withLock {
				val keyPair = TransferCodeCrypto.loadKeyPair(transferCode.code, getApplication())

				var conversionState: TransferCodeConversionState = TransferCodeConversionState.LOADING
				if (keyPair != null) {
					try {
						val decryptedCertificates = deliveryRepository.download(transferCode.code, keyPair)

						if (decryptedCertificates.isNotEmpty()) {
							var didReplaceTransferCode = false

							decryptedCertificates.forEachIndexed { index, convertedCertificate ->
								val qrCodeData = convertedCertificate.qrCodeData
								val pdfData = convertedCertificate.pdfData

								if (index == 0) {
									didReplaceTransferCode =
										walletDataStorage.replaceTransferCodeWithCertificate(transferCode, qrCodeData, pdfData)
									val decodeState = CovidCertificateSdk.Wallet.decode(qrCodeData)
									conversionState = if (decodeState is DecodeState.SUCCESS) {
										MainApplication.getTransferCodeConversionMapping(getApplication())
											?.put(transferCode.code, decodeState.certificateHolder)
										TransferCodeConversionState.CONVERTED(decodeState.certificateHolder)
									} else {
										// The certificate returned from the server could not be decoded
										TransferCodeConversionState.NOT_CONVERTED
									}
								} else {
									walletDataStorage.saveWalletDataItem(WalletDataItem.CertificateWalletData(qrCodeData, pdfData))
								}
							}

							// Delete the transfer code on the backend and the key pair only if the certificate was stored (either by the above replace method or from another thread)
							val didStoreCertificate =
								walletDataStorage.containsCertificate(decryptedCertificates.first().qrCodeData)
							if (didReplaceTransferCode || didStoreCertificate) {
								try {
									deliveryRepository.complete(transferCode.code, keyPair)
								} catch (e: IOException) {
									// This request is best-effort, if it fails, ignore it and let the backend delete the transfer code and certificate
									// automatically after it expires
								}
								TransferCodeCrypto.deleteKeyEntry(transferCode.code, getApplication())
							}
						} else {
							conversionState = TransferCodeConversionState.NOT_CONVERTED
						}
					} catch (e: TimeDeviationException) {
						conversionState = TransferCodeConversionState.ERROR(StateError(DeliveryRepository.ERROR_CODE_INVALID_TIME))
					} catch (e: IOException) {
						conversionState = if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
							TransferCodeConversionState.ERROR(StateError(ErrorCodes.GENERAL_NETWORK_FAILURE))
						} else {
							TransferCodeConversionState.ERROR(StateError(ErrorCodes.GENERAL_OFFLINE))
						}
					}
				} else {
					val alreadyLoadedCertificate =
						MainApplication.getTransferCodeConversionMapping(getApplication())?.get(transferCode.code)
					if (alreadyLoadedCertificate != null) {
						conversionState = TransferCodeConversionState.CONVERTED(alreadyLoadedCertificate)
					} else {
						conversionState =
							TransferCodeConversionState.ERROR(StateError(TransferCodeErrorCodes.INAPP_DELIVERY_KEYPAIR_GENERATION_FAILED))
					}
				}

				val updatedStatefulWalletItems = updateConversionStateForTransferCode(transferCode, conversionState)

				// Set the livedata value on the main dispatcher to prevent multiple posts overriding each other
				withContext(Dispatchers.Main.immediate) {
					statefulWalletItemsMutableLiveData.value = updatedStatefulWalletItems
				}
			}
		}
	}

	private fun updateVerificationStateForDccHolder(
		certificateHolder: CertificateHolder,
		newVerificationState: VerificationState
	): List<StatefulWalletItem> {
		val newStatefulWalletItems = statefulWalletItems.value?.toMutableList() ?: mutableListOf()
		val index = newStatefulWalletItems.indexOfFirst {
			it is StatefulWalletItem.VerifiedCertificate && it.certificateHolder == certificateHolder
		}

		if (index >= 0) {
			newStatefulWalletItems[index] =
				StatefulWalletItem.VerifiedCertificate(certificateHolder.qrCodeData, certificateHolder, newVerificationState)
		} else {
			newStatefulWalletItems.add(
				StatefulWalletItem.VerifiedCertificate(
					certificateHolder.qrCodeData,
					certificateHolder,
					newVerificationState
				)
			)
		}

		return newStatefulWalletItems
	}

	private fun updateConversionStateForTransferCode(
		transferCode: TransferCodeModel,
		newConversionState: TransferCodeConversionState
	): List<StatefulWalletItem> {
		val newStatefulWalletItems = statefulWalletItems.value?.toMutableList() ?: mutableListOf()
		val index = newStatefulWalletItems.indexOfFirst {
			it is StatefulWalletItem.TransferCodeConversionItem && it.transferCode.code == transferCode.code
		}

		if (index >= 0) {
			newStatefulWalletItems[index] = StatefulWalletItem.TransferCodeConversionItem(transferCode, newConversionState)
		} else {
			newStatefulWalletItems.add(StatefulWalletItem.TransferCodeConversionItem(transferCode, newConversionState))
		}

		return newStatefulWalletItems
	}

}