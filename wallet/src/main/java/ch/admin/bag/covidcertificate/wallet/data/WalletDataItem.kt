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

import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import com.squareup.moshi.JsonClass

sealed class WalletDataItem {
	@JsonClass(generateAdapter = true)
	data class CertificateWalletData(
		val qrCodeData: String,
		val pdfData: String? = null,
		val certificateLightData: String? = null,
		val certificateLightQrCode: String? = null,
		val language: String? = "de"
	) : WalletDataItem()

	@JsonClass(generateAdapter = true)
	data class TransferCodeWalletData(val transferCode: TransferCodeModel) : WalletDataItem()
}