/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.debug

import android.content.Context
import ch.admin.bag.covidcertificate.sdk.android.net.CertificatePinning
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder

class DebugSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<DebugSecureStorage, Context>(::DebugSecureStorage) {
		private const val PREFERENCES = "DebugSecureStorage"
		private const val KEY_CERT_PINNING_ENABLED = "KEY_CERT_PINNING_ENABLED"
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES)

	var isCertPinningEnabled: Boolean
		get() = prefs.getBoolean(KEY_CERT_PINNING_ENABLED, CertificatePinning.enabled)
		set(value) = prefs.edit().putBoolean(KEY_CERT_PINNING_ENABLED, value).apply()

}