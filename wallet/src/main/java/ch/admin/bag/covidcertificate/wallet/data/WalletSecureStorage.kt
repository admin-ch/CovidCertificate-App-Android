/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.data

import android.content.Context
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder

class WalletSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<WalletSecureStorage, Context>(::WalletSecureStorage) {
		private const val PREFERENCES = "SecureStorage"
		private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
		private const val KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA = "KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA"
		private const val KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED = "KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED"
		private const val KEY_TRANSFER_CODE_PUBLIC_KEY_PREFIX = "TRANSFER_CODE_PUBLIC_KEY_"
		private const val KEY_TRANSFER_CODE_PRIVATE_KEY_PREFIX = "TRANSFER_CODE_PRIVATE_KEY_"
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES)

	fun getOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

	fun setOnboardingCompleted(completed: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()

	fun getMigratedCertificatesToWalletData() = prefs.getBoolean(KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA, false)

	fun setMigratedCertificatesToWalletData(migrated: Boolean) = prefs.edit().putBoolean(KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA, migrated).apply()

	fun getCertificateLightUpdateboardingCompleted() = prefs.getBoolean(KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, false)

	fun setCertificateLightUpdateboardingCompleted(completed: Boolean) = prefs.edit().putBoolean(
		KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, completed
	).apply()

	fun getTransferCodePublicKey(keyAlias: String) = prefs.getString(KEY_TRANSFER_CODE_PUBLIC_KEY_PREFIX + keyAlias, null)

	fun setTransferCodePublicKey(keyAlias: String, encodedKey: String?) =
		prefs.edit().putString(KEY_TRANSFER_CODE_PUBLIC_KEY_PREFIX + keyAlias, encodedKey).apply()

	fun getTransferCodePrivateKey(keyAlias: String) = prefs.getString(KEY_TRANSFER_CODE_PRIVATE_KEY_PREFIX + keyAlias, null)

	fun setTransferCodePrivateKey(keyAlias: String, encodedKey: String?) =
		prefs.edit().putString(KEY_TRANSFER_CODE_PRIVATE_KEY_PREFIX + keyAlias, encodedKey).apply()

}