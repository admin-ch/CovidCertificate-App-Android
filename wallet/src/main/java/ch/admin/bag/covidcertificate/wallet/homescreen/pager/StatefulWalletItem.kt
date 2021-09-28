/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.homescreen.pager

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel

sealed class StatefulWalletItem {
	data class VerifiedCertificate(
		val qrCodeData: String,
		val certificateHolder: CertificateHolder?,
		val state: VerificationState
	) : StatefulWalletItem()

	data class TransferCodeConversionItem(
		val transferCode: TransferCodeModel,
		val conversionState: TransferCodeConversionState
	) : StatefulWalletItem()
}