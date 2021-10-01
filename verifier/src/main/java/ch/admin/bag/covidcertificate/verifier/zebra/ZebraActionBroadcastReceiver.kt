/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.zebra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage

class ZebraActionBroadcastReceiver(private val secureStorage: VerifierSecureStorage) : BroadcastReceiver() {

	companion object {
		private const val INTENT_ACTION = "ch.admin.bag.covidcertificate.verifier.qr.zebra"
		private const val INTENT_CATEGORY = Intent.CATEGORY_DEFAULT
		private const val KEY_ZEBRA_RESULT_DATA_STRING = "com.symbol.datawedge.data_string"
	}

	private val intentFilter = IntentFilter().apply {
		addAction(INTENT_ACTION)
		addCategory(INTENT_CATEGORY)
	}

	private var onQrCodeScanned: (String) -> Unit = {}

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.hasExtra(KEY_ZEBRA_RESULT_DATA_STRING)) {
			handleQrCodeScannedResult(intent)
		}
	}

	fun registerWith(context: Context, onQrCodeScanned: (String) -> Unit) {
		this.onQrCodeScanned = onQrCodeScanned
		if (secureStorage.hasZebraScanner()) {
			context.registerReceiver(this, intentFilter)
		}
	}

	fun unregisterWith(context: Context) {
		if (secureStorage.hasZebraScanner()) {
			context.unregisterReceiver(this)
		}
	}

	private fun handleQrCodeScannedResult(intent: Intent) {
		val qrCodeData = intent.getStringExtra(KEY_ZEBRA_RESULT_DATA_STRING)
		qrCodeData?.let {
			onQrCodeScanned.invoke(it)
		}
	}

}