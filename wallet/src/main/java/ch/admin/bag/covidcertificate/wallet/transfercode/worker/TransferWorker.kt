/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode.worker

import android.content.Context
import androidx.work.*
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.exception.TimeDeviationException
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.MainApplication
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.TransferCodeCrypto
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliveryRepository
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliverySpec
import ch.admin.bag.covidcertificate.wallet.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class TransferWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

	companion object {
		val WORKER_NAME = TransferWorker::class.java.canonicalName

		private const val DEFAULT_REPEAT_INTERVAL = 120 * 60 * 1000L
		private const val DEFAULT_FLEX_INTERVAL = 10 * 60 * 1000L
		private const val MIN_FLEX_INTERVAL = 5 * 60 * 1000L
		private const val DEFAULT_BACKOFF_DELAY = 30 * 1000L

		fun scheduleTransferWorker(context: Context, config: ConfigModel? = null) {
			val checkIntervalConfig = config?.androidTransferCheckIntervalMs
			val transferWorkRequest = PeriodicWorkRequest.Builder(
				TransferWorker::class.java,
				checkIntervalConfig ?: DEFAULT_REPEAT_INTERVAL,
				TimeUnit.MILLISECONDS,
				max(checkIntervalConfig?.let { it / 20 } ?: DEFAULT_FLEX_INTERVAL, MIN_FLEX_INTERVAL),
				TimeUnit.MILLISECONDS
			)
				.setBackoffCriteria(
					BackoffPolicy.EXPONENTIAL,
					config?.androidTransferCheckBackoffMs ?: DEFAULT_BACKOFF_DELAY,
					TimeUnit.MILLISECONDS
				)
				.build()

			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(WORKER_NAME, ExistingPeriodicWorkPolicy.KEEP, transferWorkRequest)
		}

		fun cancelScheduledTransferWorker(context: Context) {
			WorkManager.getInstance(context).cancelUniqueWork(WORKER_NAME)
		}
	}

	private val walletDataStorage = WalletDataSecureStorage.getInstance(context)
	private val deliveryRepository = DeliveryRepository.getInstance(DeliverySpec(context, BuildConfig.BASE_URL_DELIVERY))

	private val workManager = WorkManager.getInstance(context)

	override suspend fun doWork(): Result {
		return withContext(Dispatchers.IO) {
			val transferStates = walletDataStorage.getWalletData()
				.filterIsInstance<WalletDataItem.TransferCodeWalletData>()
				.map { transferCodeItem -> downloadTransferCertificate(transferCodeItem.transferCode) }
				.toSet()

			if (transferStates.contains(TransferState.ERROR)) {
				return@withContext Result.retry()
			} else if (!transferStates.contains(TransferState.NOT_AVAILABLE)) {
				workManager.cancelUniqueWork(WORKER_NAME)
			}

			if (transferStates.contains(TransferState.SUCCESS)) {
				NotificationUtil.showTransferSuccessNotification(applicationContext)
			}
			Result.success()
		}
	}

	private suspend fun downloadTransferCertificate(transferCode: TransferCodeModel): TransferState {
		if (transferCode.isFailed()) {
			//if transfercode is expired plus TRANSFER_CODE_DURATION_FAILS_AFTER_EXPIRES is over, we stop the request and return an NOT_AVAILABLE
			return TransferState.NOT_AVAILABLE
		}

		TransferCodeCrypto.getMutex(transferCode.code).withLock {
			val keyPair = TransferCodeCrypto.loadKeyPair(transferCode.code, applicationContext)

			if (keyPair != null) {
				try {
					val decryptedCertificates = deliveryRepository.download(transferCode.code, keyPair)

					return if (decryptedCertificates.isNotEmpty()) {
						var didReplaceTransferCode = false

						decryptedCertificates.forEachIndexed { index, convertedCertificate ->
							val qrCodeData = convertedCertificate.qrCodeData
							val pdfData = convertedCertificate.pdfData
							if (index == 0) {
								didReplaceTransferCode =
									walletDataStorage.replaceTransferCodeWithCertificate(transferCode, qrCodeData, pdfData)
								val decodeState = CovidCertificateSdk.Wallet.decode(qrCodeData)
								if (decodeState is DecodeState.SUCCESS) {
									MainApplication.getTransferCodeConversionMapping(applicationContext)
										?.put(transferCode.code, decodeState.certificateHolder)
								}
							} else {
								walletDataStorage.saveWalletDataItem(WalletDataItem.CertificateWalletData(qrCodeData, pdfData))
							}
						}

						// Delete the transfer code on the backend and the key pair only if the certificate was stored (either by the above replace method or from another thread)
						val didStoreCertificate = walletDataStorage.containsCertificate(decryptedCertificates.first().qrCodeData)
						if (didReplaceTransferCode || didStoreCertificate) {
							try {
								deliveryRepository.complete(transferCode.code, keyPair)
							} catch (e: IOException) {
								// Best effort only
							}
							TransferCodeCrypto.deleteKeyEntry(transferCode.code, applicationContext)
						}
						TransferState.SUCCESS
					} else {
						TransferState.NOT_AVAILABLE
					}
				} catch (e: TimeDeviationException) {
					return TransferState.ERROR
				} catch (e: IOException) {
					return TransferState.ERROR
				}
			} else {
				return TransferState.ERROR
			}
		}
	}

	private enum class TransferState {
		SUCCESS,
		NOT_AVAILABLE,
		ERROR
	}

}