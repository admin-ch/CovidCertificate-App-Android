/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.data

import android.content.Context
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder

class VerifierSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<VerifierSecureStorage, Context>(::VerifierSecureStorage) {
		private const val PREFERENCES = "SecureStorage"
		private const val KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED = "KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED"
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES)

	fun getCertificateLightUpdateboardingCompleted() = prefs.getBoolean(KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, false)

	fun setCertificateLightUpdateboardingCompleted(completed: Boolean) = prefs.edit().putBoolean(
		KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, completed
	).apply()
}