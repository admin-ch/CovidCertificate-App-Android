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
import androidx.core.content.edit
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder

class VerifierSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<VerifierSecureStorage, Context>(::VerifierSecureStorage) {
		private const val PREFERENCES = "SecureStorage"
		private const val KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED = "KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED"
		private const val KEY_AGB_UPDATEBOARDING_COMPLETED = "KEY_AGB_UPDATEBOARDIN_COMPLETED"

		private const val KEY_HAS_ZEBRA_SCANNER = "KEY_HAS_ZEBRA_SCANNER"

		private const val KEY_SELECTED_MODE = "KEY_SELECTED_MODE"
		private const val KEY_MODE_SELECTION_TIME = "KEY_MODE_SELECTION_TIME"

		private const val KEY_SHOW_COVID_NEWS = "KEY_SHOW_COVID_NEWS_FIRST_TIME"
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES)

	fun getCertificateLightUpdateboardingCompleted() = prefs.getBoolean(KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, false)

	fun setCertificateLightUpdateboardingCompleted(completed: Boolean) = prefs.edit {
		putBoolean(KEY_CERTIFICATE_LIGHT_UPDATEBOARDING_COMPLETED, completed)
	}

	fun getAgbUpdateboardingCompleted() = prefs.getBoolean(KEY_AGB_UPDATEBOARDING_COMPLETED, false)

	fun setAgbUpdateboardingCompleted(completed: Boolean) = prefs.edit {
		putBoolean(KEY_AGB_UPDATEBOARDING_COMPLETED, completed)
	}

	fun hasZebraScanner() = prefs.getBoolean(KEY_HAS_ZEBRA_SCANNER, false)

	fun setHasZebraScanner(hasZebraScanner: Boolean) = prefs.edit { putBoolean(KEY_HAS_ZEBRA_SCANNER, hasZebraScanner) }

	fun setSelectedMode(mode: String?) = prefs.edit {
		putString(KEY_SELECTED_MODE, mode)
		putLong(KEY_MODE_SELECTION_TIME, System.currentTimeMillis())
	}

	fun getSelectedMode(): String? {
		return prefs.getString(KEY_SELECTED_MODE, null)
	}

	fun resetSelectedModeIfNeeded(maxAge: Long): Boolean {
		val selectionTime = prefs.getLong(KEY_MODE_SELECTION_TIME, 0)
		return if (selectionTime < System.currentTimeMillis() - maxAge) {
			prefs.edit {
				putString(KEY_SELECTED_MODE, null)
			}
			true
		} else {
			false
		}
	}

	fun setNewsWasShown(title: String) = prefs.edit { putString(KEY_SHOW_COVID_NEWS, title) }

	fun wasNewsShown(title: String): Boolean {
		val shownNews = prefs.getString(KEY_SHOW_COVID_NEWS, "")
		if (shownNews.isNullOrEmpty()) return false
		return shownNews == title
	}
}