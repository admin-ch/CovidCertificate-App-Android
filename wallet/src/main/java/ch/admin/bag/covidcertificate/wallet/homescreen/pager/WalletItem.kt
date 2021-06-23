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

import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel

sealed class WalletItem(open val id: Int) {
	data class DccHolderItem(override val id: Int, val qrCodeData: String, val dccHolder: DccHolder?) : WalletItem(id)
	data class TransferCodeHolderItem(override val id: Int, val transferCode: TransferCodeModel) : WalletItem(id)
}