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
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.EnvironmentUtil
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.utils.CorruptedEncryptedSharedPreferencesException
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.wallet.data.CertificateStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import ch.admin.bag.covidcertificate.wallet.transfercode.worker.TransferWorker
import ch.admin.bag.covidcertificate.wallet.util.NotificationUtil

class MainApplication : Application() {

	private val mutableFailedSharedPrefs = mutableListOf<String>()
	val failedSharedPrefs: List<String> = mutableFailedSharedPrefs

	override fun onCreate() {
		super.onCreate()

		loadAllEncryptedSharedPrefs(this)

		Config.appToken = BuildConfig.SDK_APP_TOKEN
		Config.userAgent =
			UserAgentInterceptor.UserAgentGenerator { "${this.packageName};${BuildConfig.VERSION_NAME};${BuildConfig.BUILD_TIME};Android;${Build.VERSION.SDK_INT}" }

		try {
			CovidCertificateSdk.init(this, EnvironmentUtil.getSdkEnvironment())
		} catch (e: CorruptedEncryptedSharedPreferencesException) {
			mutableFailedSharedPrefs.add(e.preferencesName)
		}

		if (failedSharedPrefs.isNotEmpty()) return
		// Add anything that uses the EncryptedSharedPrefs below this line

		migrateCertificatesToWalletData()

		setupTransferWorker()
	}

	@Suppress("DEPRECATION")
	private fun migrateCertificatesToWalletData() {
		val walletStorage = WalletSecureStorage.getInstance(this)
		if (!walletStorage.getMigratedCertificatesToWalletData()) {
			val certificateStorage = CertificateStorage.getInstance(this)
			val walletDataStorage = WalletDataSecureStorage.getInstance(this)

			val certificates = certificateStorage.getCertificateList()
			certificates.reversed().map {
				val walletData = WalletDataItem.CertificateWalletData(it)
				walletDataStorage.saveWalletDataItem(walletData)
			}

			walletStorage.setMigratedCertificatesToWalletData(true)
		}
	}

	private fun setupTransferWorker() {
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
			@OnLifecycleEvent(Lifecycle.Event.ON_START)
			fun onAppStart() {
				TransferWorker.cancelScheduledTransferWorker(this@MainApplication)
			}

			@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
			fun onAppStop() {
				val hasTransferCodes = WalletDataSecureStorage.getInstance(this@MainApplication).getWalletData()
					.filterIsInstance<WalletDataItem.TransferCodeWalletData>().isNotEmpty()
				if (hasTransferCodes) {
					TransferWorker.scheduleTransferWorker(
						this@MainApplication, ConfigRepository.getCurrentConfig(this@MainApplication)
					)
				}
			}
		})
		NotificationUtil.createTransferNotificationChannel(this)
	}

	// Load all app-specific EncryptedSharedPrefs in one place to be able to easily handle any crashes
	private fun loadAllEncryptedSharedPrefs(context: Context) {
		val prefs = listOf<SingletonHolder<Any, Context>>(
			ConfigSecureStorage,
			CertificateStorage,
			WalletSecureStorage,
			WalletDataSecureStorage,
		)

		for (pref in prefs) {
			try {
				pref.getInstance(context)
			} catch (e: CorruptedEncryptedSharedPreferencesException) {
				mutableFailedSharedPrefs.add(e.preferencesName)
			}
		}
	}
}