/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesError
import ch.admin.bag.covidcertificate.eval.nationalrules.ValidityRange


sealed class DecodeState {
	data class SUCCESS(val dgc: Bagdgc) : DecodeState()
	data class ERROR(val error: Error) : DecodeState()
}

sealed class CheckSignatureState {
	object SUCCESS : CheckSignatureState()
	data class INVALID(val signatureErrorCode: String) : CheckSignatureState()
	object LOADING : CheckSignatureState()
	data class ERROR(val error: Error) : CheckSignatureState()
}

sealed class CheckRevocationState {
	object SUCCESS : CheckRevocationState()
	object INVALID : CheckRevocationState()
	object LOADING : CheckRevocationState()
	data class ERROR(val error: Error) : CheckRevocationState()
}

sealed class CheckNationalRulesState {
	data class SUCCESS(val validityRange: ValidityRange) : CheckNationalRulesState()
	data class NOT_YET_VALID(val validityRange: ValidityRange) : CheckNationalRulesState()
	data class NOT_VALID_ANYMORE(val validityRange: ValidityRange) : CheckNationalRulesState()
	data class INVALID(val nationalRulesError: NationalRulesError) : CheckNationalRulesState()
	object LOADING : CheckNationalRulesState()
	data class ERROR(val error: Error) : CheckNationalRulesState()

	fun validityRange(): ValidityRange? = when (this) {
		is NOT_VALID_ANYMORE -> validityRange
		is NOT_YET_VALID -> validityRange
		is SUCCESS -> validityRange
		else -> null
	}
}

sealed class TrustListState {
	object SUCCESS : TrustListState()
	data class ERROR(val error: Error) : TrustListState()
}

data class Error(val code: String, val message: String? = null, val bagdgc: Bagdgc? = null)
