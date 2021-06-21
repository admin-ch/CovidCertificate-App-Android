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


class CertificateStorage private constructor(context: Context) {

	companion object : SingletonHolder<CertificateStorage, Context>(::CertificateStorage) {
		const val SHARED_PREFERENCES_NAME: String = "CertificateStorageName"
		private const val SHARED_PREFERENCES_CERTIFICATES_KEY = "CertificateStorageKey"

		private val moshi = Moshi.Builder().build()
		private val certificatesAdapter =
			moshi.adapter<MutableList<String>>(Types.newParameterizedType(MutableList::class.java, String::class.java))
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, SHARED_PREFERENCES_NAME)

	fun saveCertificate(certificate: String) {
		val certificateList = getCertificateList()
		if (certificateList.contains(certificate)) {
			return
		}
		val updatedList = arrayListOf<String>()
		//adds Certificate at first position
		updatedList.add(certificate)
		updatedList.addAll(certificateList)
		updateCertificateList(updatedList)
	}

	fun containsCertificate(certificate: String): Boolean {
		return getCertificateList().contains(certificate)
	}

	fun changeCertificatePosition(oldPosition: Int, newPosition: Int) {
		val certificateList = getCertificateList()
		if (newPosition < 0 || oldPosition < 0) {
			return
		}
		val moveCertificate: String = certificateList.removeAt(oldPosition)
		certificateList.add(newPosition, moveCertificate)
		updateCertificateList(certificateList)
	}

	fun deleteCertificate(certificate: String) {
		val bfsIds = getCertificateList()
		bfsIds.remove(certificate)
		updateCertificateList(bfsIds)
	}

	private fun updateCertificateList(certificates: MutableList<String>) {
		val bfsIdsJson = certificatesAdapter.toJson(certificates)
		val editor = prefs.edit()
		editor.putString(SHARED_PREFERENCES_CERTIFICATES_KEY, bfsIdsJson)
		editor.apply()
	}


	fun getCertificateList(): MutableList<String> {
		val json = prefs.getString(SHARED_PREFERENCES_CERTIFICATES_KEY, null)
		if (json == null || json.isEmpty()) {
			return arrayListOf()
		}
		return certificatesAdapter.fromJson(json) ?: arrayListOf()
	}

}