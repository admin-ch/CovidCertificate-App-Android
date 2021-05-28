/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.verification

import ch.admin.bag.covidcertificate.eval.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.Error
import ch.admin.bag.covidcertificate.eval.nationalrules.ValidityRange

sealed class VerificationState {
	data class SUCCESS(val validityRange: ValidityRange) : VerificationState()
	data class INVALID(
		val signatureState: CheckSignatureState?,
		val revocationState: CheckRevocationState?,
		val nationalRulesState: CheckNationalRulesState?,
		val validityRange: ValidityRange?
	) : VerificationState()

	object LOADING : VerificationState()
	data class ERROR(val error: Error, val retry: Runnable, val validityRange: ValidityRange?) : VerificationState()
}