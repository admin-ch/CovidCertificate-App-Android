/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.util

import android.content.Context
import android.text.SpannableString
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState

const val DATE_REPLACEMENT_STRING = "{DATE}"

fun VerificationState.getStatusString(context: Context): SpannableString {
	return when (this) {
		is VerificationState.SUCCESS -> context.getString(R.string.verifier_verify_success_title).makeBold()
		is VerificationState.ERROR -> context.getString(R.string.verifier_verify_error_list_title).makeBold()
		is VerificationState.INVALID -> context.getString(R.string.verifier_verify_error_title).makeBold()
		VerificationState.LOADING -> SpannableString(context.getString(R.string.wallet_certificate_verifying))
	}
}

fun VerificationState.getStatusInformationString(context: Context, errorDelimiter: String = "\n"): String {
	return when (this) {
		is VerificationState.ERROR -> context.getString(R.string.verifier_verify_error_list_info_text)
		is VerificationState.INVALID -> {
			val errorStrings = mutableListOf<String>()
			if (this.signatureState is CheckSignatureState.INVALID) {
				errorStrings.add(context.getString(R.string.verifier_verify_error_info_for_certificate_invalid))
			}
			if (this.revocationState == CheckRevocationState.INVALID) {
				errorStrings.add(context.getString(R.string.verifier_verify_error_info_for_blacklist))
			}
			if (this.nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE) {
				errorStrings.add(context.getString(R.string.verifier_verifiy_error_expired))
			}
			if (this.nationalRulesState is CheckNationalRulesState.NOT_YET_VALID) {
				errorStrings.add(context.getString(R.string.verifier_verifiy_error_notyetvalid))
			}
			if (this.nationalRulesState is CheckNationalRulesState.INVALID) {
				errorStrings.add(context.getString(R.string.verifier_verify_error_info_for_national_rules))
			}

			if (errorStrings.size <= 1) errorStrings[0] else errorStrings.map { "• $it" }.joinToString(errorDelimiter)
		}
		VerificationState.LOADING -> context.getString(R.string.wallet_certificate_verifying)
		is VerificationState.SUCCESS -> context.getString(R.string.verifier_verify_success_info)
	}
}

fun VerificationState.getInvalidErrorCode(errorDelimiter: String = ", ") : String {
	var errorCodes = mutableListOf<String>()
	if (this !is VerificationState.INVALID) return ""

	val signatureState = signatureState
	if (signatureState is CheckSignatureState.INVALID) {
		errorCodes.add(signatureState.signatureErrorCode)
	}
	val nationalRulesState = nationalRulesState
	if (nationalRulesState is CheckNationalRulesState.INVALID) {
		errorCodes.add(nationalRulesState.nationalRulesError.errorCode)
	}
	return errorCodes.joinToString(errorDelimiter)
}

@DrawableRes
fun VerificationState.getValidationStatusIcon(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.drawable.ic_process_error
		is VerificationState.INVALID -> R.drawable.ic_error
		is VerificationState.SUCCESS -> R.drawable.ic_check_green
		VerificationState.LOADING -> 0
	}
}

@DrawableRes
fun VerificationState.getValidationStatusIconLarge(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.drawable.ic_process_error_large
		is VerificationState.INVALID -> R.drawable.ic_error_large
		VerificationState.LOADING -> 0
		is VerificationState.SUCCESS -> R.drawable.ic_check_large
	}
}

@ColorRes
fun VerificationState.getStatusBubbleColor(isInfoBubble: Boolean = false): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orangeish
		is VerificationState.INVALID -> R.color.redish
		VerificationState.LOADING -> R.color.greyish
		is VerificationState.SUCCESS -> if (isInfoBubble) R.color.blueish else R.color.greenish
	}
}

@ColorRes
fun VerificationState.getInfoIconColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> R.color.blue
	}
}

@ColorRes
fun VerificationState.getHeaderColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> R.color.green
	}
}