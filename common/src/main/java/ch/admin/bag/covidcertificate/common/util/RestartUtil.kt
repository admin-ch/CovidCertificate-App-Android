/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.util

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import com.jakewharton.processphoenix.ProcessPhoenix

object RestartUtil {

	fun showRestartDialogForSharedPrefCrash(activity: Activity, failedSharedPrefs: List<String>) {
		AlertDialog.Builder(activity)
			.setTitle("Entschlüsselungsfehler")
			.setMessage("Ein Speicher der App konnte nicht entschlüsselt werden. Um die App weiter zu benutzen muss der Speicher gelöscht und neu erstellt werden. Dabei gehen Daten verloren.")
			.setPositiveButton(activity.resources.getString(R.string.delete_button)) { _, _ ->
				failedSharedPrefs.forEach { prefName ->
					EncryptedSharedPreferencesUtil.tryToDeleteSharedPreferencesFile(activity, prefName)
				}
				ProcessPhoenix.triggerRebirth(activity)
			}
			.setNegativeButton(activity.resources.getString(R.string.cancel_button)) { _, _ ->
				activity.finish()
			}
			.setCancelable(false)
			.create()
			.show()
	}
}

