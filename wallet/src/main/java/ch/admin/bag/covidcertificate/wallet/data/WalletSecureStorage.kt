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
import androidx.core.content.edit
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Duration
import java.time.Instant

class WalletSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<WalletSecureStorage, Context>(::WalletSecureStorage) {
		private const val PREFERENCES = "SecureStorage"
		private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
		private const val KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA = "KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA"
		private const val KEY_MIGRATED_TRANSFER_CODE_VALIDITY = "KEY_MIGRATED_TRANSFER_CODE_VALIDITY"
		private const val KEY_MIGRATED_TRANSFER_CODE_FAILS_AT = "KEY_MIGRATED_TRANSFER_CODE_FAILS_AT"
		private const val KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED = "KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED"
		private const val KEY_TRANSFER_CODE_PUBLIC_KEY_PREFIX = "TRANSFER_CODE_PUBLIC_KEY_"
		private const val KEY_TRANSFER_CODE_PRIVATE_KEY_PREFIX = "TRANSFER_CODE_PRIVATE_KEY_"
		private const val KEY_VACCINATION_HINT_DISMISS_TIMESTAMP = "KEY_VACCINATION_HINT_DISMISS_TIMESTAMP"
		private const val KEY_LAST_SCAN_TIMES = "KEY_LAST_SCAN_TIMES"
		private val moshi = Moshi.Builder().build()
		private val longListAdapter =
			moshi.adapter<List<Long>>(Types.newParameterizedType(List::class.java, Long::class.javaObjectType))
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES)

	fun getOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

	fun setOnboardingCompleted(completed: Boolean) = prefs.edit {
		putBoolean(KEY_ONBOARDING_COMPLETED, completed)
	}

	fun getMigratedCertificatesToWalletData() = prefs.getBoolean(KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA, false)

	fun setMigratedCertificatesToWalletData(migrated: Boolean) = prefs.edit {
		putBoolean(KEY_MIGRATED_CERTIFICATES_TO_WALLET_DATA, migrated)
	}

	fun getMigratedTransferCodeValidity() = prefs.getBoolean(KEY_MIGRATED_TRANSFER_CODE_VALIDITY, false)

	fun setMigratedTransferCodeValidity(migrated: Boolean) = prefs.edit {
		putBoolean(KEY_MIGRATED_TRANSFER_CODE_VALIDITY, migrated)
	}

	fun getMigratedTransferCodeFailsAt() = prefs.getBoolean(KEY_MIGRATED_TRANSFER_CODE_FAILS_AT, false)

	fun setMigratedTransferCodeFailsAt(migrated: Boolean) = prefs.edit {
		putBoolean(KEY_MIGRATED_TRANSFER_CODE_FAILS_AT, migrated)
	}

	fun getCertificateLightUpdateboardingCompleted() = prefs.getBoolean(KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, false)

	fun setCertificateLightUpdateboardingCompleted(completed: Boolean) = prefs.edit {
		putBoolean(KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, completed)
	}

	fun getVaccinationHintDismissTimestamp() = prefs.getLong(KEY_VACCINATION_HINT_DISMISS_TIMESTAMP, 0L)

	fun setVaccinationHintDismissTimestamp(timestamp: Long) = prefs.edit {
		putLong(KEY_VACCINATION_HINT_DISMISS_TIMESTAMP, timestamp)
	}

	/*
	 * The following methods are only used on Android 6 as a workaround to the AndroidKeyStore not properly working with the
	 * signing and decrypting functionality of the In-App Delivery feature. On later Android versions, the key pairs are directly
	 * stored in the AndroidKeyStore.
	 */
	fun getTransferCodePublicKey(keyAlias: String) = prefs.getString(KEY_TRANSFER_CODE_PUBLIC_KEY_PREFIX + keyAlias, null)

	fun setTransferCodePublicKey(keyAlias: String, encodedKey: String?) = prefs.edit {
		putString(KEY_TRANSFER_CODE_PUBLIC_KEY_PREFIX + keyAlias, encodedKey)
	}

	fun getTransferCodePrivateKey(keyAlias: String) = prefs.getString(KEY_TRANSFER_CODE_PRIVATE_KEY_PREFIX + keyAlias, null)

	fun setTransferCodePrivateKey(keyAlias: String, encodedKey: String?) = prefs.edit {
		putString(KEY_TRANSFER_CODE_PRIVATE_KEY_PREFIX + keyAlias, encodedKey)
	}

	fun addLastScanTimes(time: Long) {
		val times = getLastScanTimes().toMutableList()
		times.add(time)
		prefs.edit {
			putString(KEY_LAST_SCAN_TIMES, longListAdapter.toJson(times))
		}
	}

	private fun getLastScanTimes(): List<Long> {
		val times = longListAdapter.fromJson(prefs.getString(KEY_LAST_SCAN_TIMES, "[]")) ?: emptyList()
		return times.sortedDescending().filterIndexed { index, _ -> index < 10 }
	}

	fun has10OrMoreScansInLast24h(): Boolean {
		return getLastScanTimes().filter { time ->
			Instant.ofEpochMilli(time).isAfter(Instant.now().minus(Duration.ofDays(1)))
		}.size >= 10
	}

	fun resetScanCount() {
		prefs.edit {
			putString(KEY_LAST_SCAN_TIMES, longListAdapter.toJson(emptyList()))
		}
	}

}