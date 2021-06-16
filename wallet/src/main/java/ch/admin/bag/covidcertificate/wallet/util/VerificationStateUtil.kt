/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.util

import android.content.Context
import android.text.SpannableString
import androidx.annotation.ColorRes
import ch.admin.bag.covidcertificate.common.util.addBoldDate
import ch.admin.bag.covidcertificate.common.util.makeSubStringBold
import ch.admin.bag.covidcertificate.eval.data.EvalErrorCodes
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.R

const val DATE_REPLACEMENT_STRING = "{DATE}"

/**
 * The verification state indicates an offline mode if it is an ERROR and the error code is set to GENERAL_OFFLINE (G|OFF)
 */
fun VerificationState.isOfflineMode() = this is VerificationState.ERROR && this.error.code == EvalErrorCodes.GENERAL_OFFLINE

fun VerificationState.INVALID.getValidationStatusString(context: Context) = when {
	signatureState is CheckSignatureState.INVALID -> {
		val invalidSignatureState = signatureState as CheckSignatureState.INVALID
		if (invalidSignatureState.signatureErrorCode == EvalErrorCodes.SIGNATURE_TYPE_INVALID) {
			context.getString(R.string.wallet_error_invalid_format)
				.makeSubStringBold(context.getString(R.string.wallet_error_invalid_format_bold))
		} else {
			context.getString(R.string.wallet_error_invalid_signature)
				.makeSubStringBold(context.getString(R.string.wallet_error_invalid_signature_bold))
		}
	}
	revocationState == CheckRevocationState.INVALID -> {
		context.getString(R.string.wallet_error_revocation)
			.makeSubStringBold(context.getString(R.string.wallet_error_revocation_bold))
	}
	nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
		context.getString(R.string.wallet_error_expired)
			.makeSubStringBold(context.getString(R.string.wallet_error_expired_bold))
	}
	nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> {
		context.getString(R.string.wallet_error_valid_from).addBoldDate(
			DATE_REPLACEMENT_STRING,
			(nationalRulesState as CheckNationalRulesState.NOT_YET_VALID).validityRange.validFrom!!
		)
	}
	nationalRulesState is CheckNationalRulesState.INVALID -> {
		SpannableString(context.getString(R.string.wallet_error_national_rules))
	}
	else -> SpannableString(context.getString(R.string.unknown_error))
}

@ColorRes
fun VerificationState.getNameDobColor(): Int {
	return when (this) {
		is VerificationState.INVALID -> R.color.grey
		else -> R.color.black
	}
}

fun VerificationState.getQrAlpha(): Float {
	return when (this) {
		is VerificationState.INVALID -> 0.55f
		else -> 1f
	}
}
