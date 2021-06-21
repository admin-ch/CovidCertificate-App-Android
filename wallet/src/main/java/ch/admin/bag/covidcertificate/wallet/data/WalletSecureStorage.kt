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
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import java.io.IOException
import java.security.GeneralSecurityException

class WalletSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<WalletSecureStorage, Context>(::WalletSecureStorage) {

		private const val PREFERENCES = "SecureStorage"
		private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
	}

	private val prefs: SharedPreferences

	init {
		prefs = initializeSharedPreferences(context)
	}

	@Synchronized
	private fun initializeSharedPreferences(context: Context): SharedPreferences {
		return try {
			createEncryptedSharedPreferences(context)
		} catch (e: GeneralSecurityException) {
			throw RuntimeException(e)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}

	/**
	 * Create or obtain an encrypted SharedPreferences instance. Note that this method is synchronized because the AndroidX
	 * Security
	 * library is not thread-safe.
	 * @see [https://developer.android.com/topic/security/data](https://developer.android.com/topic/security/data)
	 */
	@Synchronized
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
		val masterKey = MasterKey.Builder(context)
			.setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
			.build()
		return EncryptedSharedPreferences.create(
			context,
			PREFERENCES,
			masterKey,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
		)
	}


	fun getOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

	fun setOnboardingCompleted(completed: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()

}