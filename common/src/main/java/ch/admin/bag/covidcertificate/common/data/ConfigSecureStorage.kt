/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.data

import android.content.Context
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.eval.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import com.squareup.moshi.Moshi

class ConfigSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<ConfigSecureStorage, Context>(::ConfigSecureStorage) {
		private const val PREFERENCES = "ConfigSecureStorage"

		private const val KEY_CONFIG = "ConfigKey"
		private const val KEY_CONFIG_LAST_SUCCESS = "LastSuccessTimestampKey"
		private const val KEY_CONFIG_LAST_SUCCESS_APP_AND_OS_VERSION = "LastSuccessVersionKey"
		private const val KEY_CONFIG_SHOWN_INFO_BOX_ID = "LastShownInfoBoxId"

		private val moshi = Moshi.Builder().build()
		private val configModelAdapter = moshi.adapter(ConfigModel::class.java)
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, PREFERENCES)

	fun updateConfigData(config: ConfigModel, timestamp: Long, appVersion: String) {
		val editor = prefs.edit()
		editor.putLong(KEY_CONFIG_LAST_SUCCESS, timestamp)
		editor.putString(KEY_CONFIG_LAST_SUCCESS_APP_AND_OS_VERSION, appVersion)
		editor.putString(KEY_CONFIG, configModelAdapter.toJson(config))
		editor.apply()
	}

	fun getConfig(): ConfigModel? =
		prefs.getString(KEY_CONFIG, null)
			?.let { configModelAdapter.fromJson(it) }

	fun getConfigLastSuccessTimestamp(): Long =
		prefs.getLong(KEY_CONFIG_LAST_SUCCESS, 0)

	fun getConfigLastSuccessAppAndOSVersion(): String? =
		prefs.getString(KEY_CONFIG_LAST_SUCCESS_APP_AND_OS_VERSION, null)

	fun setLastShownInfoBoxId(infoBoxId: Long) = prefs.edit().putLong(KEY_CONFIG_SHOWN_INFO_BOX_ID, infoBoxId).apply()

	fun getLastShownInfoBoxId(): Long = prefs.getLong(KEY_CONFIG_SHOWN_INFO_BOX_ID, 0L)
}