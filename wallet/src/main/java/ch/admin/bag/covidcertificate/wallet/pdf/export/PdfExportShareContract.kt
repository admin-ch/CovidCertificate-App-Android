/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.pdf.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import ch.admin.bag.covidcertificate.wallet.R

class PdfExportShareContract : ActivityResultContract<Uri, Uri>() {

	private var uri: Uri? = null

	override fun createIntent(context: Context, input: Uri?): Intent {
		this.uri = input
		val intent = Intent(Intent.ACTION_SEND).apply {
			type = "application/pdf"
			putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.wallet_certificate))
			putExtra(Intent.EXTRA_STREAM, input)
		}
		return Intent.createChooser(intent, null)
	}

	override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
		return uri
	}
}