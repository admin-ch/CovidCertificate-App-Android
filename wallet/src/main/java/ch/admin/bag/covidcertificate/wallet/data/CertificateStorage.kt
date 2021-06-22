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
import ch.admin.bag.covidcertificate.eval.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Deprecated("With the addition of transfer codes, this storage has been deprecated in favor of the more generic WalletDataSecureStorage")
class CertificateStorage private constructor(context: Context) {

	companion object : SingletonHolder<CertificateStorage, Context>(::CertificateStorage) {
		const val SHARED_PREFERENCES_NAME: String = "CertificateStorageName"
		private const val SHARED_PREFERENCES_CERTIFICATES_KEY = "CertificateStorageKey"

		private val moshi = Moshi.Builder().build()
		private val certificatesAdapter =
			moshi.adapter<MutableList<String>>(Types.newParameterizedType(MutableList::class.java, String::class.java))
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, SHARED_PREFERENCES_NAME)

	fun getCertificateList(): MutableList<String> {
		val json = prefs.getString(SHARED_PREFERENCES_CERTIFICATES_KEY, null)
		if (json == null || json.isEmpty()) {
			return arrayListOf()
		}
		return certificatesAdapter.fromJson(json) ?: arrayListOf()
	}

}