/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.light.model

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError

sealed class CertificateLightConversionState {
	object LOADING : CertificateLightConversionState()
	data class SUCCESS(val certificateHolder: CertificateHolder, val qrCodeImage: String) : CertificateLightConversionState()
	data class ERROR(val error: StateError) : CertificateLightConversionState()
}
